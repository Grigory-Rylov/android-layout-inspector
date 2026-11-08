package com.github.grishberg.android.layoutinspector.compose

import com.android.layoutinspector.common.AppLogger
import com.github.grishberg.android.layoutinspector.compose.ViewInspectionCodec.Rect
import com.github.grishberg.android.layoutinspector.compose.ViewInspectionCodec.ViewEvent
import com.google.protobuf.CodedOutputStream
import java.io.ByteArrayOutputStream
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.runBlocking
import org.junit.Test

class ViewInspectionCodecTest {

    private fun ByteArray.hex(): String = joinToString("") { "%02x".format(it) }

    @Test
    fun `start fetch command exact bytes`() {
        assertEquals("0a00", ViewInspectionCodec.encodeStartFetchCommand(false).hex())
        assertEquals("0a020801", ViewInspectionCodec.encodeStartFetchCommand(true).hex())
    }

    @Test
    fun `roots event round trip with packed ids`() {
        val event = ViewEvent.WindowRoots(listOf(1234567890123L, -42L, 7L))
        val decoded = ViewInspectionCodec.decodeEventBytes(ViewInspectionCodec.encodeEvent(event))
        assertEquals(event, decoded)
    }

    @Test
    fun `roots event decodes unpacked repeated ids`() {
        // WindowRootsEvent{ ids: 10, 20 } written unpacked, wrapped into Event{roots_event=2}
        val inner = ByteArrayOutputStream()
        val innerCoded = CodedOutputStream.newInstance(inner)
        innerCoded.writeInt64(1, 10L)
        innerCoded.writeInt64(1, 20L)
        innerCoded.flush()
        val outer = ByteArrayOutputStream()
        val coded = CodedOutputStream.newInstance(outer)
        coded.writeByteArray(2, inner.toByteArray())
        coded.flush()
        val decoded = ViewInspectionCodec.decodeEventBytes(outer.toByteArray())
        assertEquals(ViewEvent.WindowRoots(listOf(10L, 20L)), decoded)
    }

    @Test
    fun `layout event decodes root id class name and bounds`() {
        val event = ViewEvent.Layout(
            rootViewId = -900L,
            rootClassName = "com.example.MainActivity\$MainContent",
            rootBounds = Rect(0, 0, 1080, 1920),
        )
        val decoded = ViewInspectionCodec.decodeEventBytes(ViewInspectionCodec.encodeEvent(event))
        assertEquals(event, decoded)
    }

    @Test
    fun `error event and unknown arms decode`() {
        val error = ViewInspectionCodec.decodeEventBytes(
            ViewInspectionCodec.encodeEvent(ViewEvent.Error("boom"))
        )
        assertEquals(ViewEvent.Error("boom"), error)
        val unknown = ViewInspectionCodec.decodeEventBytes(byteArrayOf(0x2a, 0x00)) // field 5 LEN
        assertEquals(ViewEvent.Other(5), unknown)
    }

    @Test
    fun `start fetch response error decodes`() {
        // Response{ start_fetch_response=1 { error=1 "nope" } }
        val inner = ByteArrayOutputStream()
        val innerCoded = CodedOutputStream.newInstance(inner)
        innerCoded.writeString(1, "nope")
        innerCoded.flush()
        val outer = ByteArrayOutputStream()
        val coded = CodedOutputStream.newInstance(outer)
        coded.writeByteArray(1, inner.toByteArray())
        coded.flush()
        assertEquals("nope", ViewInspectionCodec.decodeStartFetchResponseError(outer.toByteArray()))
        assertEquals("", ViewInspectionCodec.decodeStartFetchResponseError(byteArrayOf(0x0a, 0x00)).orEmpty())
        assertNull(ViewInspectionCodec.decodeStartFetchResponseError(byteArrayOf()))
    }

    // ----- collectRootViewIds logic (ComposeCaptureService) -----

    private fun flowOfLater(vararg events: ByteArray): Flow<ByteArray> = callbackFlow {
        // emit synchronously-ish through a buffer so the collector subscribes first
        events.forEach { trySend(it) }
        // keep the flow open until the collector cancels (like the real event flow)
        kotlinx.coroutines.awaitCancellation()
    }.buffer(Channel.UNLIMITED)

    private object TestLogger : AppLogger {
        override fun d(msg: String) = Unit
        override fun e(msg: String) = Unit
        override fun e(msg: String, t: Throwable) = Unit
        override fun w(msg: String) = Unit
    }

    private val service = ComposeCaptureService(TestLogger)

    @Test
    fun `collect returns ids from early roots event without start fetch`() = runBlocking {
        var fetched = false
        val ids = service.collectRootViewIds(
            events = flowOfLater(
                ViewInspectionCodec.encodeEvent(ViewEvent.WindowRoots(listOf(11L, 22L)))
            ),
            sendStartFetch = { fetched = true },
            rootsWindowMs = 200,
            fetchWindowMs = 200,
        )
        assertEquals(setOf(11L, 22L), ids)
        assertEquals(false, fetched)
    }

    @Test
    fun `collect falls back to start fetch and takes layout root id`() = runBlocking {
        var fetched = false
        val ids = service.collectRootViewIds(
            events = flowOfLater(
                // no roots event at all; after start fetch a layout event carries the id
                ViewInspectionCodec.encodeEvent(
                    ViewEvent.Layout(rootViewId = 77L, rootClassName = null, rootBounds = null)
                ),
            ),
            sendStartFetch = { fetched = true },
            rootsWindowMs = 50,
            fetchWindowMs = 500,
        )
        assertEquals(true, fetched)
        assertTrue("expected 77 in $ids", 77L in ids)
    }

    @Test
    fun `collect returns empty when nothing arrives`() = runBlocking {
        val ids = service.collectRootViewIds(
            events = flowOfLater(byteArrayOf(0x2a, 0x00)), // ProgressEvent (unmodelled arm)
            sendStartFetch = { },
            rootsWindowMs = 30,
            fetchWindowMs = 60,
        )
        assertEquals(emptySet<Long>(), ids)
    }
}
