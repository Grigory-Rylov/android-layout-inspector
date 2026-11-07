package com.github.grishberg.android.layoutinspector.compose

import com.google.protobuf.CodedInputStream
import com.google.protobuf.CodedOutputStream
import java.io.ByteArrayOutputStream

/**
 * Hand-written protobuf codec for the *view* inspector protocol
 * (`layoutinspector.view.inspection`), vendored as reference in
 * `src/main/proto/view_layout_inspection.proto` (protoc-lite messages, package
 * `layoutinspector.view.inspection`).
 *
 * YALI only needs a sliver of that protocol: launch the view inspector to learn the window root
 * view ids (`View.getUniqueDrawingId()` values the compose agent keys on), which arrive either
 * as `Event{roots_event: WindowRootsEvent{repeated int64 ids=1}}` or, after sending
 * `Command{start_fetch_command: StartFetchCommand{continuous=false}}`, inside
 * `Event{layout_event: LayoutEvent{root_view{node{id}}}}`.
 *
 * Wire facts used here (view_layout_inspection.proto):
 *  - Event.oneof specialized: ErrorEvent=1, WindowRootsEvent(roots_event)=2,
 *    LayoutEvent(layout_event)=3, PropertiesEvent=4, ProgressEvent=5, UserInputEvent=6
 *  - WindowRootsEvent.ids = 1 (repeated int64, packed by default)
 *  - LayoutEvent.strings = 1 (repeated StringEntry), root_view = 4
 *  - RootView.node = 1; ViewNode.id = 1 (int64), children = 2, class_name = 5 (string-table id),
 *    bounds = 6; Bounds.layout = 1 (Rect x=1,y=2,w=3,h=4)
 *  - Command.oneof: StartFetchCommand=1; StartFetchCommand.continuous = 1 (bool)
 *  - Response.oneof: StartFetchResponse=1; StartFetchResponse.error = 1 (string, empty=ok)
 *
 * Like [ComposeInspectionCodec] this file has no Android Studio imports so it unit-tests without
 * the IDE. The event/message bytes here are the *raw* payloads of the app-inspection transport
 * (same RawCommand/RawResponse envelope finding as in [ComposeCaptureService]).
 */
object ViewInspectionCodec {

    private const val WIRE_VARINT = 0
    private const val WIRE_LENGTH_DELIMITED = 2

    // Event oneof
    private const val EVENT_ERROR = 1
    private const val EVENT_ROOTS = 2
    private const val EVENT_LAYOUT = 3

    // ----- public model (only what YALI consumes) -----

    sealed class ViewEvent {
        /** Window root View.getUniqueDrawingId() values. */
        data class WindowRoots(val ids: List<Long>) : ViewEvent()

        /** One fetched layout tree; [rootViewId] is LayoutEvent.root_view.node.id. */
        data class Layout(
            val rootViewId: Long?,
            val rootClassName: String?,
            val rootBounds: Rect?,
        ) : ViewEvent()

        /** ErrorEvent from the agent. */
        data class Error(val message: String) : ViewEvent()

        /** An event arm YALI does not model (properties/progress/user input). */
        data class Other(val fieldNumber: Int) : ViewEvent()
    }

    data class Rect(val x: Int, val y: Int, val w: Int, val h: Int)

    /** Decodes one raw `Event` message from the view inspector's event flow. */
    fun decodeEventBytes(bytes: ByteArray): ViewEvent? {
        val input = CodedInputStream.newInstance(bytes)
        var event: ViewEvent? = null
        while (true) {
            val tag = input.readTag()
            if (tag == 0) break
            when (tag) {
                (EVENT_ERROR shl 3) or WIRE_LENGTH_DELIMITED ->
                    event = readErrorEvent(input.readByteArray())
                (EVENT_ROOTS shl 3) or WIRE_LENGTH_DELIMITED ->
                    event = ViewEvent.WindowRoots(
                        readInt64List(CodedInputStream.newInstance(input.readByteArray()))
                    )
                (EVENT_LAYOUT shl 3) or WIRE_LENGTH_DELIMITED ->
                    event = readLayoutEvent(CodedInputStream.newInstance(input.readByteArray()))
                else -> {
                    if (event == null) event = ViewEvent.Other(tag ushr 3)
                    input.skipField(tag)
                }
            }
        }
        return event
    }

    /** Decodes `Response.start_fetch_response.error`; null = no error / unrecognised. */
    fun decodeStartFetchResponseError(bytes: ByteArray): String? {
        val input = CodedInputStream.newInstance(bytes)
        var error: String? = null
        while (true) {
            val tag = input.readTag()
            if (tag == 0) break
            if (tag == (1 shl 3) or WIRE_LENGTH_DELIMITED) {
                val inner = input.readByteArray()
                val innerInput = CodedInputStream.newInstance(inner)
                while (true) {
                    val innerTag = innerInput.readTag()
                    if (innerTag == 0) break
                    if (innerTag == (1 shl 3) or WIRE_LENGTH_DELIMITED) {
                        val message = innerInput.readStringRequireUtf8()
                        if (message.isNotEmpty()) error = message
                    } else {
                        innerInput.skipField(innerTag)
                    }
                }
            } else {
                input.skipField(tag)
            }
        }
        return error
    }

    /** Encodes `Command{start_fetch_command{continuous}}`. */
    fun encodeStartFetchCommand(continuous: Boolean): ByteArray {
        val inner = ByteArrayOutputStream()
        if (continuous) {
            val out = CodedOutputStream.newInstance(inner)
            out.writeBool(1, true)
            out.flush()
        }
        val bytes = inner.toByteArray()
        val out = ByteArrayOutputStream()
        val coded = CodedOutputStream.newInstance(out)
        coded.writeByteArray(1, bytes)
        coded.flush()
        return out.toByteArray()
    }

    // ----- test/event fixtures builders (used by unit tests, harmless in prod) -----

    fun encodeEvent(event: ViewEvent): ByteArray {
        val payload: ByteArray
        val field: Int
        when (event) {
            is ViewEvent.WindowRoots -> {
                val inner = ByteArrayOutputStream()
                val innerCoded = CodedOutputStream.newInstance(inner)
                for (id in event.ids) innerCoded.writeInt64(1, id)
                innerCoded.flush()
                payload = inner.toByteArray()
                field = EVENT_ROOTS
            }
            is ViewEvent.Error -> {
                val inner = ByteArrayOutputStream()
                val innerCoded = CodedOutputStream.newInstance(inner)
                innerCoded.writeString(1, event.message)
                innerCoded.flush()
                payload = inner.toByteArray()
                field = EVENT_ERROR
            }
            is ViewEvent.Layout -> {
                payload = encodeLayoutEvent(event)
                field = EVENT_LAYOUT
            }
            is ViewEvent.Other -> throw IllegalArgumentException("cannot encode Other")
        }
        val out = ByteArrayOutputStream()
        val coded = CodedOutputStream.newInstance(out)
        coded.writeByteArray(field, payload)
        coded.flush()
        return out.toByteArray()
    }

    private fun encodeLayoutEvent(event: ViewEvent.Layout): ByteArray {
        var classId = 0
        var stringEntryBytes: ByteArray? = null
        event.rootClassName?.let { name ->
            stringEntryBytes = encodeStringEntry(1, name)
            classId = 1
        }

        val rect = ByteArrayOutputStream()
        val rectCoded = CodedOutputStream.newInstance(rect)
        event.rootBounds?.let { b ->
            rectCoded.writeInt32(1, b.x)
            rectCoded.writeInt32(2, b.y)
            rectCoded.writeInt32(3, b.w)
            rectCoded.writeInt32(4, b.h)
        }
        rectCoded.flush()
        val bounds = ByteArrayOutputStream()
        val boundsCoded = CodedOutputStream.newInstance(bounds)
        if (event.rootBounds != null) boundsCoded.writeByteArray(1, rect.toByteArray())
        boundsCoded.flush()

        val node = ByteArrayOutputStream()
        val nodeCoded = CodedOutputStream.newInstance(node)
        event.rootViewId?.let { nodeCoded.writeInt64(1, it) }
        if (classId != 0) nodeCoded.writeInt32(5, classId)
        if (event.rootBounds != null) nodeCoded.writeByteArray(6, bounds.toByteArray())
        nodeCoded.flush()

        val rootView = ByteArrayOutputStream()
        val rootCoded = CodedOutputStream.newInstance(rootView)
        rootCoded.writeByteArray(1, node.toByteArray())
        rootCoded.flush()

        val layout = ByteArrayOutputStream()
        val layoutCoded = CodedOutputStream.newInstance(layout)
        // repeated StringEntry: one field-1 occurrence per entry (no extra wrapper message)
        stringEntryBytes?.let { layoutCoded.writeByteArray(1, it) }
        layoutCoded.writeByteArray(4, rootView.toByteArray())
        layoutCoded.flush()
        return layout.toByteArray()
    }

    private fun encodeStringEntry(id: Int, value: String): ByteArray {
        val out = ByteArrayOutputStream()
        val coded = CodedOutputStream.newInstance(out)
        coded.writeInt32(1, id)
        coded.writeString(2, value)
        coded.flush()
        return out.toByteArray()
    }

    // ----- decoding helpers -----

    private fun readErrorEvent(bytes: ByteArray): ViewEvent.Error {
        val input = CodedInputStream.newInstance(bytes)
        var message = ""
        while (true) {
            val tag = input.readTag()
            if (tag == 0) break
            if (tag == (1 shl 3) or WIRE_LENGTH_DELIMITED) message = input.readStringRequireUtf8()
            else input.skipField(tag)
        }
        return ViewEvent.Error(message)
    }

    private fun readInt64List(input: CodedInputStream): List<Long> {
        val ids = mutableListOf<Long>()
        while (true) {
            val tag = input.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or WIRE_LENGTH_DELIMITED -> {
                    val packed = input.readByteArray()
                    val packedInput = CodedInputStream.newInstance(packed)
                    while (!packedInput.isAtEnd) ids += packedInput.readInt64()
                }
                (1 shl 3) or WIRE_VARINT -> ids += input.readInt64()
                else -> input.skipField(tag)
            }
        }
        return ids
    }

    private fun readLayoutEvent(input: CodedInputStream): ViewEvent.Layout {
        var strings: Map<Int, String> = emptyMap()
        var rootViewId: Long? = null
        var classId = 0
        var bounds: Rect? = null
        while (true) {
            val tag = input.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or WIRE_LENGTH_DELIMITED -> {
                    val (id, value) = readStringEntry(input.readByteArray())
                    strings = strings + (id to value)
                }
                (4 shl 3) or WIRE_LENGTH_DELIMITED -> {
                    // RootView { node=1 }
                    val rootViewInput = CodedInputStream.newInstance(input.readByteArray())
                    while (true) {
                        val rootTag = rootViewInput.readTag()
                        if (rootTag == 0) break
                        if (rootTag == (1 shl 3) or WIRE_LENGTH_DELIMITED) {
                            // ViewNode (top level only: id, class_name, bounds.layout)
                            val nodeInput = CodedInputStream.newInstance(rootViewInput.readByteArray())
                            while (true) {
                                val nodeTag = nodeInput.readTag()
                                if (nodeTag == 0) break
                                when (nodeTag) {
                                    (1 shl 3) or WIRE_VARINT -> rootViewId = nodeInput.readInt64()
                                    (5 shl 3) or WIRE_VARINT -> classId = nodeInput.readInt32()
                                    (6 shl 3) or WIRE_LENGTH_DELIMITED ->
                                        bounds = readBounds(nodeInput.readByteArray())
                                    else -> nodeInput.skipField(nodeTag)
                                }
                            }
                        } else {
                            rootViewInput.skipField(rootTag)
                        }
                    }
                }
                else -> input.skipField(tag)
            }
        }
        return ViewEvent.Layout(
            rootViewId = rootViewId,
            rootClassName = strings[classId].takeIf { classId != 0 },
            rootBounds = bounds,
        )
    }

    private fun readBounds(bytes: ByteArray): Rect? {
        val input = CodedInputStream.newInstance(bytes)
        var rectBytes: ByteArray? = null
        while (true) {
            val tag = input.readTag()
            if (tag == 0) break
            if (tag == (1 shl 3) or WIRE_LENGTH_DELIMITED) rectBytes = input.readByteArray()
            else input.skipField(tag)
        }
        rectBytes ?: return null
        val rect = CodedInputStream.newInstance(rectBytes)
        var x = 0; var y = 0; var w = 0; var h = 0
        while (true) {
            val tag = rect.readTag()
            if (tag == 0) break
            when (tag) {
                (1 shl 3) or WIRE_VARINT -> x = rect.readInt32()
                (2 shl 3) or WIRE_VARINT -> y = rect.readInt32()
                (3 shl 3) or WIRE_VARINT -> w = rect.readInt32()
                (4 shl 3) or WIRE_VARINT -> h = rect.readInt32()
                else -> rect.skipField(tag)
            }
        }
        return Rect(x, y, w, h)
    }

    private fun readStringEntry(bytes: ByteArray): Pair<Int, String> {
        val entryInput = CodedInputStream.newInstance(bytes)
        var id = 0
        var value = ""
        while (true) {
            val entryTag = entryInput.readTag()
            if (entryTag == 0) break
            when (entryTag) {
                (1 shl 3) or WIRE_VARINT -> id = entryInput.readInt32()
                (2 shl 3) or WIRE_LENGTH_DELIMITED -> value = entryInput.readStringRequireUtf8()
                else -> entryInput.skipField(entryTag)
            }
        }
        return id to value
    }
}
