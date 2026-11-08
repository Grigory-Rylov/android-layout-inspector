package com.github.grishberg.android.layoutinspector.compose

import com.google.protobuf.CodedInputStream
import com.google.protobuf.CodedOutputStream
import java.io.ByteArrayOutputStream

/**
 * Hand-written protobuf (de)serializer for the Jetpack Compose layout inspection wire protocol
 * (see src/main/proto/compose_layout_inspection.proto, package layoutinspector.compose.inspection).
 *
 * The protocol is transported by Android Studio's App Inspection framework: YALI sends/receives the
 * *packed bytes of the compose [Command]/[Response] messages* through
 * `AppInspectorMessenger.sendRawCommand()`; the RawCommand / AppInspectionCommand envelope is built
 * and unwrapped internally by DefaultAppInspectionTransport (verified from AppInspectorConnection
 * bytecode: sendRawCommand wraps the payload into AppInspectionCommand{rawInspectorCommand=
 * RawCommand{content}} and returns AppInspectionResponse.rawResponse.content verbatim).
 *
 * Only the messages needed for a single-shot hierarchy capture are implemented:
 * Command{get_composables_command|update_settings_command} and
 * Response{get_composables_response|update_settings_response|unknown_command_response}.
 * Strings inside GetComposablesResponse are table-indexed (StringEntry), node ids are sint (zigzag).
 *
 * Pure Kotlin + protobuf-java low-level streams; no Android Studio classes, so it unit-tests
 * without an IDE.
 */
object ComposeInspectionCodec {

    // ==== decoded model (decoupled from protobuf / Android Studio) ====

    /** Layout-space bounds as reported by the inspector agent — absolute screen pixels. */
    data class ComposeRect(val x: Int, val y: Int, val width: Int, val height: Int) {
        val left: Int get() = x
        val top: Int get() = y
        val right: Int get() = x + width
        val bottom: Int get() = y + height
    }

    /** A single composable in the hierarchy. [name]/[file] are already resolved via the string table. */
    data class ComposeNode(
        val id: Long,
        val name: String,
        val file: String,
        val line: Int,
        val offset: Int,
        val bounds: ComposeRect,
        val flags: Int,
        val viewId: Long,
        val recomposeCount: Int,
        val recomposeSkips: Int,
        val packageHash: Int,
        val anchorHash: Int,
        val children: List<ComposeNode>,
    )

    /**
     * One extracted parameter / semantics property value (see proto `Parameter`). [name] is the
     * string-table-resolved value: the parameter name for named parameters, and - matching the
     * agent's serializer - the string value itself for STRING-typed values without a name.
     */
    data class ComposeParameter(
        val type: Int,
        val name: String,
        val elements: List<ComposeParameter>,
    )

    /** Parameters of one composable, incl. the semantics lists that carry e.g. Text values. */
    data class ParameterGroup(
        val composableId: Long,
        val parameter: List<ComposeParameter>,
        val mergedSemantics: List<ComposeParameter>,
        val unmergedSemantics: List<ComposeParameter>,
    )

    /** Composable trees owned by one host view (usually a single AndroidComposeView). */
    data class ComposableRoot(
        val viewId: Long,
        val nodes: List<ComposeNode>,
        val viewsToSkip: List<Long>,
    )
    sealed class ComposeResponse {
        data class GetComposables(
            val roots: List<ComposableRoot>,
            val unchanged: Boolean,
        ) : ComposeResponse()

        data class UpdateSettings(
            val canDelayParameterExtractions: Boolean,
        ) : ComposeResponse()

        /** Parsed GetAllParametersResponse (semantics / parameter values keyed by composable id). */
        data class GetAllParameters(
            val rootViewId: Long,
            val groups: List<ParameterGroup>,
        ) : ComposeResponse()

        /** The agent could not parse our command - usually an inspector/agent version mismatch. */
        data class Unknown(val commandBytes: ByteArray) : ComposeResponse()

        /** Any other oneof arm of Response (parameters, state reads); not used for capture. */
        data class Unsupported(val fieldNumber: Int) : ComposeResponse()
    }

    // ==== wire constants ====

    private const val FIELD_GET_COMPOSABLES = 1
    private const val FIELD_UPDATE_SETTINGS = 5
    private const val FIELD_GET_ALL_PARAMETERS = 3

    private const val WIRE_VARINT = 0
    private const val WIRE_LENGTH_DELIMITED = 2

    // ==== commands ====

    /**
     * Command{get_composables_command{root_view_id, skip_system_composables, generation}}.
     * proto3: default values are not serialized.
     */
    fun encodeGetComposablesCommand(
        rootViewId: Long = 0L,
        skipSystemComposables: Boolean = true,
        generation: Int = 0,
    ): ByteArray {
        val inner = writeToMessageSize(
            (if (rootViewId != 0L) CodedOutputStream.computeInt64Size(1, rootViewId) else 0) +
                (if (skipSystemComposables) CodedOutputStream.computeBoolSize(2, true) else 0) +
                (if (generation != 0) CodedOutputStream.computeInt32Size(3, generation) else 0)
        ) { out ->
            if (rootViewId != 0L) out.writeInt64(1, rootViewId)
            if (skipSystemComposables) out.writeBool(2, true)
            if (generation != 0) out.writeInt32(3, generation)
        }
        return nested(FIELD_GET_COMPOSABLES, inner)
    }

    /**
     * Command{get_all_parameters_command{root_view_id, skip_system_composables, ...}}: asks the
     * agent to extract the parameter / semantics values (incl. the `Text` property) for every
     * composable of one window root. Recursion / iterable limits stay at the proto defaults.
     */
    fun encodeGetAllParametersCommand(
        rootViewId: Long,
        skipSystemComposables: Boolean = true,
    ): ByteArray {
        val inner = writeToMessageSize(
            (if (rootViewId != 0L) CodedOutputStream.computeInt64Size(1, rootViewId) else 0) +
                (if (skipSystemComposables) CodedOutputStream.computeBoolSize(2, true) else 0)
        ) { out ->
            if (rootViewId != 0L) out.writeInt64(1, rootViewId)
            if (skipSystemComposables) out.writeBool(2, true)
        }
        return nested(FIELD_GET_ALL_PARAMETERS, inner)
    }

    /** Command{update_settings_command{...}} (optional; supported for completeness / counters setup). */
    fun encodeUpdateSettingsCommand(
        includeRecomposeCounts: Boolean,
        keepRecomposeCounts: Boolean = false,
        delayParameterExtractions: Boolean = true,
        reduceChildNesting: Boolean = false,
    ): ByteArray {
        val inner = writeToMessageSize(
            (if (includeRecomposeCounts) CodedOutputStream.computeBoolSize(1, true) else 0) +
                (if (keepRecomposeCounts) CodedOutputStream.computeBoolSize(2, true) else 0) +
                (if (delayParameterExtractions) CodedOutputStream.computeBoolSize(3, true) else 0) +
                (if (reduceChildNesting) CodedOutputStream.computeBoolSize(4, true) else 0)
        ) { out ->
            if (includeRecomposeCounts) out.writeBool(1, true)
            if (keepRecomposeCounts) out.writeBool(2, true)
            if (delayParameterExtractions) out.writeBool(3, true)
            if (reduceChildNesting) out.writeBool(4, true)
        }
        return nested(FIELD_UPDATE_SETTINGS, inner)
    }

    // ==== responses ====

    fun decodeResponse(bytes: ByteArray): ComposeResponse {
        val input = CodedInputStream.newInstance(bytes)
        var response: ComposeResponse? = null
        while (true) {
            val tag = input.readTag()
            if (tag == 0) {
                break
            }
            when (tag) {
                (FIELD_GET_COMPOSABLES shl 3) or WIRE_LENGTH_DELIMITED ->
                    response = decodeGetComposablesResponse(input.readByteArray())
                (FIELD_UPDATE_SETTINGS shl 3) or WIRE_LENGTH_DELIMITED ->
                    response = decodeUpdateSettingsResponse(input.readByteArray())
                (FIELD_GET_ALL_PARAMETERS shl 3) or WIRE_LENGTH_DELIMITED ->
                    response = decodeGetAllParametersResponse(input.readByteArray())
                (100 shl 3) or WIRE_LENGTH_DELIMITED -> { // unknown_command_response
                    var commandBytes = ByteArray(0)
                    val inner = CodedInputStream.newInstance(input.readByteArray())
                    while (true) {
                        val innerTag = inner.readTag()
                        if (innerTag == 0) {
                            break
                        }
                        if (innerTag == (1 shl 3) or WIRE_LENGTH_DELIMITED) {
                            commandBytes = inner.readByteArray()
                        } else {
                            inner.skipField(innerTag)
                        }
                    }
                    response = ComposeResponse.Unknown(commandBytes)
                }
                else -> {
                    response = ComposeResponse.Unsupported(tag ushr 3)
                    input.skipField(tag)
                }
            }
        }
        return response ?: ComposeResponse.Unsupported(0)
    }

    private fun decodeGetComposablesResponse(bytes: ByteArray): ComposeResponse.GetComposables {
        val strings = mutableMapOf<Int, String>()
        val roots = mutableListOf<ComposableRoot>()
        var unchanged = false
        val input = CodedInputStream.newInstance(bytes)
        while (true) {
            val tag = input.readTag()
            if (tag == 0) {
                break
            }
            when (tag) {
                (1 shl 3) or WIRE_LENGTH_DELIMITED -> { // repeated StringEntry strings
                    val entry = decodeStringEntry(input.readByteArray())
                    strings[entry.first] = entry.second
                }
                (2 shl 3) or WIRE_LENGTH_DELIMITED ->
                    roots.add(decodeComposableRoot(input.readByteArray(), strings))
                (3 shl 3) or WIRE_VARINT -> unchanged = input.readBool()
                else -> input.skipField(tag)
            }
        }
        return ComposeResponse.GetComposables(roots, unchanged)
    }

    private fun decodeUpdateSettingsResponse(bytes: ByteArray): ComposeResponse.UpdateSettings {
        var canDelay = false
        val input = CodedInputStream.newInstance(bytes)
        while (true) {
            val tag = input.readTag()
            if (tag == 0) {
                break
            }
            when (tag) {
                (3 shl 3) or WIRE_VARINT -> canDelay = input.readBool()
                else -> input.skipField(tag)
            }
        }
        return ComposeResponse.UpdateSettings(canDelay)
    }

    /**
     * GetAllParametersResponse{int64 root_view_id = 1; repeated StringEntry strings = 2;
     * repeated ParameterGroup parameter_groups = 3} - note the different field numbers from
     * GetComposablesResponse (strings are field 2 here).
     */
    private fun decodeGetAllParametersResponse(bytes: ByteArray): ComposeResponse.GetAllParameters {
        val strings = mutableMapOf<Int, String>()
        val groups = mutableListOf<ParameterGroup>()
        var rootViewId = 0L
        val input = CodedInputStream.newInstance(bytes)
        while (true) {
            val tag = input.readTag()
            if (tag == 0) {
                break
            }
            when (tag) {
                (1 shl 3) or WIRE_VARINT -> rootViewId = input.readInt64()
                (2 shl 3) or WIRE_LENGTH_DELIMITED -> {
                    val entry = decodeStringEntry(input.readByteArray())
                    strings[entry.first] = entry.second
                }
                (3 shl 3) or WIRE_LENGTH_DELIMITED ->
                    groups.add(decodeParameterGroup(input.readByteArray(), strings))
                else -> input.skipField(tag)
            }
        }
        return ComposeResponse.GetAllParameters(rootViewId, groups)
    }

    private fun decodeParameterGroup(bytes: ByteArray, strings: Map<Int, String>): ParameterGroup {
        var composableId = 0L
        val parameter = mutableListOf<ComposeParameter>()
        val mergedSemantics = mutableListOf<ComposeParameter>()
        val unmergedSemantics = mutableListOf<ComposeParameter>()
        val input = CodedInputStream.newInstance(bytes)
        while (true) {
            val tag = input.readTag()
            if (tag == 0) {
                break
            }
            when (tag) {
                (1 shl 3) or WIRE_VARINT -> composableId = input.readSInt64()
                (3 shl 3) or WIRE_LENGTH_DELIMITED -> parameter.add(decodeParameter(input.readByteArray(), strings))
                (4 shl 3) or WIRE_LENGTH_DELIMITED -> mergedSemantics.add(decodeParameter(input.readByteArray(), strings))
                (5 shl 3) or WIRE_LENGTH_DELIMITED -> unmergedSemantics.add(decodeParameter(input.readByteArray(), strings))
                else -> input.skipField(tag)
            }
        }
        return ParameterGroup(composableId, parameter, mergedSemantics, unmergedSemantics)
    }

    private fun decodeParameter(bytes: ByteArray, strings: Map<Int, String>): ComposeParameter {
        var type = 0
        var name = 0
        val elements = mutableListOf<ComposeParameter>()
        val input = CodedInputStream.newInstance(bytes)
        while (true) {
            val tag = input.readTag()
            if (tag == 0) {
                break
            }
            when (tag) {
                (1 shl 3) or WIRE_VARINT -> type = input.readInt32()
                (2 shl 3) or WIRE_VARINT -> name = input.readInt32()
                (3 shl 3) or WIRE_LENGTH_DELIMITED -> elements.add(decodeParameter(input.readByteArray(), strings))
                else -> input.skipField(tag) // reference, index, value oneof
            }
        }
        return ComposeParameter(type, strings[name].orEmpty(), elements)
    }

    private fun decodeStringEntry(bytes: ByteArray): Pair<Int, String> {
        var id = 0
        var str = ""
        val input = CodedInputStream.newInstance(bytes)
        while (true) {
            val tag = input.readTag()
            if (tag == 0) {
                break
            }
            when (tag) {
                (1 shl 3) or WIRE_VARINT -> id = input.readInt32()
                (2 shl 3) or WIRE_LENGTH_DELIMITED -> str = input.readStringRequireUtf8()
                else -> input.skipField(tag)
            }
        }
        return id to str
    }

    private fun decodeComposableRoot(bytes: ByteArray, strings: Map<Int, String>): ComposableRoot {
        var viewId = 0L
        val nodes = mutableListOf<ComposeNode>()
        val viewsToSkip = mutableListOf<Long>()
        val input = CodedInputStream.newInstance(bytes)
        while (true) {
            val tag = input.readTag()
            if (tag == 0) {
                break
            }
            when (tag) {
                (1 shl 3) or WIRE_VARINT -> viewId = input.readInt64()
                (2 shl 3) or WIRE_LENGTH_DELIMITED -> nodes.add(decodeComposableNode(input.readByteArray(), strings))
                (3 shl 3) or WIRE_LENGTH_DELIMITED -> { // packed repeated int64
                    val inner = CodedInputStream.newInstance(input.readByteArray())
                    while (!inner.isAtEnd) {
                        viewsToSkip.add(inner.readInt64())
                    }
                }
                (3 shl 3) or WIRE_VARINT -> viewsToSkip.add(input.readInt64()) // unpacked form
                else -> input.skipField(tag)
            }
        }
        return ComposableRoot(viewId, nodes, viewsToSkip)
    }

    private fun decodeComposableNode(bytes: ByteArray, strings: Map<Int, String>): ComposeNode {
        var id = 0L
        var packageHash = 0
        var filename = 0
        var lineNumber = 0
        var offset = 0
        var name = 0
        var bounds = ComposeRect(0, 0, 0, 0)
        var flags = 0
        var viewId = 0L
        var recomposeCount = 0
        var recomposeSkips = 0
        var anchorHash = 0
        val children = mutableListOf<ComposeNode>()
        val input = CodedInputStream.newInstance(bytes)
        while (true) {
            val tag = input.readTag()
            if (tag == 0) {
                break
            }
            when (tag) {
                (1 shl 3) or WIRE_VARINT -> id = input.readSInt64()
                (2 shl 3) or WIRE_LENGTH_DELIMITED -> children.add(decodeComposableNode(input.readByteArray(), strings))
                (3 shl 3) or WIRE_VARINT -> packageHash = input.readInt32()
                (4 shl 3) or WIRE_VARINT -> filename = input.readInt32()
                (5 shl 3) or WIRE_VARINT -> lineNumber = input.readInt32()
                (6 shl 3) or WIRE_VARINT -> offset = input.readInt32()
                (7 shl 3) or WIRE_VARINT -> name = input.readInt32()
                (8 shl 3) or WIRE_LENGTH_DELIMITED -> bounds = decodeBounds(input.readByteArray())
                (9 shl 3) or WIRE_VARINT -> flags = input.readInt32()
                (10 shl 3) or WIRE_VARINT -> viewId = input.readInt64()
                (11 shl 3) or WIRE_VARINT -> recomposeCount = input.readInt32()
                (12 shl 3) or WIRE_VARINT -> recomposeSkips = input.readInt32()
                (13 shl 3) or WIRE_VARINT -> anchorHash = input.readSInt32()
                else -> input.skipField(tag)
            }
        }
        return ComposeNode(
            id = id,
            name = strings[name].orEmpty(),
            file = strings[filename].orEmpty(),
            line = lineNumber,
            offset = offset,
            bounds = bounds,
            flags = flags,
            viewId = viewId,
            recomposeCount = recomposeCount,
            recomposeSkips = recomposeSkips,
            packageHash = packageHash,
            anchorHash = anchorHash,
            children = children,
        )
    }

    /** Bounds{Rect layout = 1; Quad render = 2}; only the layout rect is interesting for YALI. */
    private fun decodeBounds(bytes: ByteArray): ComposeRect {
        val input = CodedInputStream.newInstance(bytes)
        var rect = ComposeRect(0, 0, 0, 0)
        while (true) {
            val tag = input.readTag()
            if (tag == 0) {
                break
            }
            when (tag) {
                (1 shl 3) or WIRE_LENGTH_DELIMITED -> rect = decodeRect(input.readByteArray())
                else -> input.skipField(tag) // render quad and anything else
            }
        }
        return rect
    }

    private fun decodeRect(bytes: ByteArray): ComposeRect {
        var x = 0
        var y = 0
        var w = 0
        var h = 0
        val input = CodedInputStream.newInstance(bytes)
        while (true) {
            val tag = input.readTag()
            if (tag == 0) {
                break
            }
            when (tag) {
                (1 shl 3) or WIRE_VARINT -> x = input.readInt32()
                (2 shl 3) or WIRE_VARINT -> y = input.readInt32()
                (3 shl 3) or WIRE_VARINT -> w = input.readInt32()
                (4 shl 3) or WIRE_VARINT -> h = input.readInt32()
                else -> input.skipField(tag)
            }
        }
        return ComposeRect(x, y, w, h)
    }

    // ==== response encoding (test fixtures / tooling; the real agent sends this form) ====

    /**
     * Encodes a GetComposablesResponse for the given roots, rebuilding a StringEntry table from the
     * distinct node names/files. Together with [decodeResponse] this gives a lossless round trip of
     * the subset modeled by [ComposeNode].
     */
    fun encodeGetComposablesResponse(
        roots: List<ComposableRoot>,
        unchanged: Boolean = false,
    ): ByteArray {
        val strings = linkedMapOf<String, Int>()
        var nextId = 1
        fun stringId(value: String): Int = strings.getOrPut(value) { nextId++ }
        roots.forEach { root -> root.nodes.forEach { collectStringIds(it, ::stringId) } }

        val stringEntriesSize = strings.entries.sumOf { (value, id) ->
            val entrySize = (if (id != 0) CodedOutputStream.computeInt32Size(1, id) else 0) +
                CodedOutputStream.computeStringSize(2, value)
            nestedSize(1, entrySize)
        }
        val rootsSize = roots.sumOf { root -> nestedSize(2, encodeRootSize(root, strings)) }
        val unchangedSize = if (unchanged) CodedOutputStream.computeBoolSize(3, true) else 0

        val payload = writeToMessageSize(stringEntriesSize + rootsSize + unchangedSize) { out ->
            strings.entries.sortedBy { it.value }.forEach { (value, id) ->
                out.writeByteArray(1, writeToMessageSize(
                    CodedOutputStream.computeInt32Size(1, id) +
                        CodedOutputStream.computeStringSize(2, value)
                ) { e ->
                    e.writeInt32(1, id)
                    e.writeString(2, value)
                })
            }
            roots.forEach { root ->
                out.writeByteArray(2, encodeRootBytes(root, strings))
            }
            if (unchanged) out.writeBool(3, true)
        }
        return nested(FIELD_GET_COMPOSABLES, payload)
    }

    private fun collectStringIds(node: ComposeNode, stringId: (String) -> Int) {
        stringId(node.name)
        stringId(node.file)
        node.children.forEach { collectStringIds(it, stringId) }
    }

    private fun encodeRootSize(root: ComposableRoot, strings: Map<String, Int>): Int {
        var size = 0
        if (root.viewId != 0L) {
            size += CodedOutputStream.computeInt64Size(1, root.viewId)
        }
        root.nodes.forEach { size += nestedSize(2, encodeNodeSize(it, strings)) }
        if (root.viewsToSkip.isNotEmpty()) {
            // packed encoding
            val packed = root.viewsToSkip.sumOf { CodedOutputStream.computeInt64SizeNoTag(it) }
            size += nestedSize(3, packed)
        }
        return size
    }

    private fun encodeRootBytes(root: ComposableRoot, strings: Map<String, Int>): ByteArray =
        writeToMessageSize(encodeRootSize(root, strings)) { out ->
            if (root.viewId != 0L) {
                out.writeInt64(1, root.viewId)
            }
            root.nodes.forEach { out.writeByteArray(2, encodeNodeBytes(it, strings)) }
            if (root.viewsToSkip.isNotEmpty()) {
                val packed = ByteArrayOutputStream(CodedOutputStream.computeUInt32SizeNoTag(
                    root.viewsToSkip.sumOf { CodedOutputStream.computeInt64SizeNoTag(it) }
                ) + root.viewsToSkip.sumOf { CodedOutputStream.computeInt64SizeNoTag(it) })
                val packedOut = CodedOutputStream.newInstance(packed)
                root.viewsToSkip.forEach { packedOut.writeInt64NoTag(it) }
                packedOut.flush()
                out.writeByteArray(3, packed.toByteArray())
            }
        }

    private fun encodeNodeSize(node: ComposeNode, strings: Map<String, Int>): Int {
        var size = 0
        if (node.id != 0L) size += CodedOutputStream.computeSInt64Size(1, node.id)
        node.children.forEach { size += nestedSize(2, encodeNodeSize(it, strings)) }
        if (node.packageHash != 0) size += CodedOutputStream.computeInt32Size(3, node.packageHash)
        val fileId = strings[node.file] ?: 0
        if (fileId != 0) size += CodedOutputStream.computeInt32Size(4, fileId)
        if (node.line != 0) size += CodedOutputStream.computeInt32Size(5, node.line)
        if (node.offset != 0) size += CodedOutputStream.computeInt32Size(6, node.offset)
        val nameId = strings[node.name] ?: 0
        if (nameId != 0) size += CodedOutputStream.computeInt32Size(7, nameId)
        if (node.bounds.width != 0 || node.bounds.height != 0 || node.bounds.x != 0 || node.bounds.y != 0) {
            size += nestedSize(8, encodeBoundsSize(node.bounds))
        }
        if (node.flags != 0) size += CodedOutputStream.computeInt32Size(9, node.flags)
        if (node.viewId != 0L) size += CodedOutputStream.computeInt64Size(10, node.viewId)
        if (node.recomposeCount != 0) size += CodedOutputStream.computeInt32Size(11, node.recomposeCount)
        if (node.recomposeSkips != 0) size += CodedOutputStream.computeInt32Size(12, node.recomposeSkips)
        if (node.anchorHash != 0) size += CodedOutputStream.computeSInt32Size(13, node.anchorHash)
        return size
    }

    private fun encodeNodeBytes(node: ComposeNode, strings: Map<String, Int>): ByteArray =
        writeToMessageSize(encodeNodeSize(node, strings)) { out ->
            if (node.id != 0L) out.writeSInt64(1, node.id)
            node.children.forEach { out.writeByteArray(2, encodeNodeBytes(it, strings)) }
            if (node.packageHash != 0) out.writeInt32(3, node.packageHash)
            val fileId = strings[node.file] ?: 0
            if (fileId != 0) out.writeInt32(4, fileId)
            if (node.line != 0) out.writeInt32(5, node.line)
            if (node.offset != 0) out.writeInt32(6, node.offset)
            val nameId = strings[node.name] ?: 0
            if (nameId != 0) out.writeInt32(7, nameId)
            if (node.bounds.width != 0 || node.bounds.height != 0 || node.bounds.x != 0 || node.bounds.y != 0) {
                out.writeByteArray(8, writeToMessageSize(encodeBoundsSize(node.bounds)) { b ->
                    b.writeByteArray(1, encodeBoundsRectBytes(node.bounds))
                })
            }
            if (node.flags != 0) out.writeInt32(9, node.flags)
            if (node.viewId != 0L) out.writeInt64(10, node.viewId)
            if (node.recomposeCount != 0) out.writeInt32(11, node.recomposeCount)
            if (node.recomposeSkips != 0) out.writeInt32(12, node.recomposeSkips)
            if (node.anchorHash != 0) out.writeSInt32(13, node.anchorHash)
        }

    private fun encodeBoundsSize(rect: ComposeRect): Int =
        CodedOutputStream.computeTagSize(1) +
            CodedOutputStream.computeUInt32SizeNoTag(encodeBoundsRectSize(rect)) +
            encodeBoundsRectSize(rect)

    private fun encodeBoundsRectSize(rect: ComposeRect): Int {
        var size = 0
        if (rect.x != 0) size += CodedOutputStream.computeInt32Size(1, rect.x)
        if (rect.y != 0) size += CodedOutputStream.computeInt32Size(2, rect.y)
        if (rect.width != 0) size += CodedOutputStream.computeInt32Size(3, rect.width)
        if (rect.height != 0) size += CodedOutputStream.computeInt32Size(4, rect.height)
        return size
    }

    private fun encodeBoundsRectBytes(rect: ComposeRect): ByteArray =
        writeToMessageSize(encodeBoundsRectSize(rect)) { out ->
            if (rect.x != 0) out.writeInt32(1, rect.x)
            if (rect.y != 0) out.writeInt32(2, rect.y)
            if (rect.width != 0) out.writeInt32(3, rect.width)
            if (rect.height != 0) out.writeInt32(4, rect.height)
        }

    // ==== low-level helpers ====

    private fun nestedSize(fieldNumber: Int, payloadSize: Int): Int =
        CodedOutputStream.computeTagSize(fieldNumber) +
            CodedOutputStream.computeUInt32SizeNoTag(payloadSize) + payloadSize

    /** Wraps an already-sized payload as a length-delimited value of [fieldNumber] in the parent. */
    private fun nested(fieldNumber: Int, payload: ByteArray): ByteArray {
        val out = ByteArrayOutputStream(nestedSize(fieldNumber, payload.size))
        val stream = CodedOutputStream.newInstance(out)
        stream.writeByteArray(fieldNumber, payload)
        stream.flush()
        return out.toByteArray()
    }

    /**
     * Writes a message body sized exactly [size] bytes; a mismatch is a bug in the size computation
     * above and fails fast with IllegalStateException (from protobuf-java) instead of producing
     * silently corrupt wire data.
     */
    private inline fun writeToMessageSize(size: Int, write: (CodedOutputStream) -> Unit): ByteArray {
        val out = ByteArrayOutputStream(size)
        val stream = CodedOutputStream.newInstance(out)
        write(stream)
        stream.flush()
        val bytes = out.toByteArray()
        check(bytes.size == size) { "compose codec size mismatch: computed $size, written ${bytes.size}" }
        return bytes
    }
}
