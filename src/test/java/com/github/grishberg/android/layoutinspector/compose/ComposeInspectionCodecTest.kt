package com.github.grishberg.android.layoutinspector.compose

import com.github.grishberg.android.layoutinspector.compose.ComposeInspectionCodec.ComposeNode
import com.github.grishberg.android.layoutinspector.compose.ComposeInspectionCodec.ComposeParameter
import com.github.grishberg.android.layoutinspector.compose.ComposeInspectionCodec.ComposeRect
import com.github.grishberg.android.layoutinspector.compose.ComposeInspectionCodec.ComposeResponse
import com.github.grishberg.android.layoutinspector.compose.ComposeInspectionCodec.ComposableRoot
import com.github.grishberg.android.layoutinspector.compose.ComposeInspectionCodec.ParameterGroup
import com.google.protobuf.CodedOutputStream
import java.io.ByteArrayOutputStream
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test

/**
 * Wire-format tests for [ComposeInspectionCodec]: exact command bytes against the proto spec and
 * a full GetComposablesResponse round trip (string table, zigzag ids, nested nodes, bounds).
 */
class ComposeInspectionCodecTest {

    private fun hex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it) }

    @Test
    fun `get composables command exact bytes`() {
        // Command{get_composables_command{skip_system_composables=true}}
        // field 1 length-delimited -> tag 0x0a; inner field 2 varint true -> 0x10 0x01
        assertEquals("0a021001", hex(ComposeInspectionCodec.encodeGetComposablesCommand()))
    }

    @Test
    fun `get composables command with root view id`() {
        // Command{get_composables_command{root_view_id=5, skip_system_composables=true}}
        // inner: 08 05 (int64 root_view_id=5) 10 01 (bool) -> len 4
        assertEquals(
            "0a0408051001",
            hex(ComposeInspectionCodec.encodeGetComposablesCommand(rootViewId = 5)),
        )
    }

    @Test
    fun `update settings command exact bytes`() {
        // Command{update_settings_command{include_recompose_counts=true, delay_parameter_extractions=true}}
        assertEquals(
            // field 5 length-delimited -> tag (5<<3)|2 = 0x2a; inner: 08 01 18 01
            "2a0408011801",
            hex(ComposeInspectionCodec.encodeUpdateSettingsCommand(includeRecomposeCounts = true)),
        )
    }

    @Test
    fun `get composables response round trip`() {
        val leaf1 = ComposeNode(
            id = -9_007_199_254_740_993, // negative sint64, needs zigzag
            name = "Text",
            file = "MainActivity.kt",
            line = 42,
            offset = 1234,
            bounds = ComposeRect(-8, -16, 100, 40), // negative offsets are legal
            flags = 0x8, // INLINED
            viewId = 101,
            recomposeCount = 3,
            recomposeSkips = 1,
            packageHash = 0x7ABCDEF0,
            anchorHash = -42,
            children = emptyList(),
        )
        val leaf2 = ComposeNode(
            id = 12345,
            name = "Button",
            file = "MainActivity.kt",
            line = 55,
            offset = 0,
            bounds = ComposeRect(0, 40, 200, 80),
            flags = 0,
            viewId = 101,
            recomposeCount = 0,
            recomposeSkips = 0,
            packageHash = 0,
            anchorHash = 0,
            children = emptyList(),
        )
        val column = ComposeNode(
            id = 99,
            name = "Column",
            file = "MainActivity.kt",
            line = 20,
            offset = 4,
            bounds = ComposeRect(0, 0, 1080, 400),
            flags = 0x20,
            viewId = 101,
            recomposeCount = 1,
            recomposeSkips = 0,
            packageHash = 0,
            anchorHash = 55,
            children = listOf(leaf1, leaf2),
        )
        val otherRoot = ComposeNode(
            id = 500,
            name = "App",
            file = "/home/dev/App.kt",
            line = 10,
            offset = 0,
            bounds = ComposeRect(0, 0, 1080, 1920),
            flags = 0x1,
            viewId = 202,
            recomposeCount = 7,
            recomposeSkips = 2,
            packageHash = 1,
            anchorHash = -1,
            children = emptyList(),
        )
        val expected = listOf(
            ComposableRoot(viewId = 101, nodes = listOf(column), viewsToSkip = listOf(7, 8, 9)),
            ComposableRoot(viewId = 202, nodes = listOf(otherRoot), viewsToSkip = emptyList()),
        )

        val encoded = ComposeInspectionCodec.encodeGetComposablesResponse(expected)
        val decoded = ComposeInspectionCodec.decodeResponse(encoded)

        assertTrue("expected GetComposables, got $decoded", decoded is ComposeResponse.GetComposables)
        val getComposables = decoded as ComposeResponse.GetComposables
        assertEquals(expected, getComposables.roots)
        assertEquals(false, getComposables.unchanged)
    }

    @Test
    fun `unchanged response round trip`() {
        val encoded = ComposeInspectionCodec.encodeGetComposablesResponse(emptyList(), unchanged = true)
        val decoded = ComposeInspectionCodec.decodeResponse(encoded)
        assertTrue(decoded is ComposeResponse.GetComposables)
        assertTrue((decoded as ComposeResponse.GetComposables).unchanged)
        assertTrue(decoded.roots.isEmpty())
    }

    @Test
    fun `unknown command response decoding`() {
        // hand-crafted Response{unknown_command_response=100{command_bytes=1=[0xde,0xad]}}
        val inner = byteArrayOf(0x0a.toByte(), 0x02, 0xde.toByte(), 0xad.toByte())
        val out = ByteArrayOutputStream()
        val stream = CodedOutputStream.newInstance(out)
        stream.writeByteArray(100, inner)
        stream.flush()
        val decoded = ComposeInspectionCodec.decodeResponse(out.toByteArray())
        assertTrue("expected Unknown, got $decoded", decoded is ComposeResponse.Unknown)
        val payload = (decoded as ComposeResponse.Unknown).commandBytes
        assertEquals("dead", payload.joinToString("") { "%02x".format(it) })
    }

    @Test
    fun `other oneof arms decode as unsupported`() {
        // Response{get_parameters_response=2{}}
        val out = ByteArrayOutputStream()
        val stream = CodedOutputStream.newInstance(out)
        stream.writeByteArray(2, ByteArray(0))
        stream.flush()
        val decoded = ComposeInspectionCodec.decodeResponse(out.toByteArray())
        assertEquals(ComposeResponse.Unsupported(2), decoded)
    }

    @Test
    fun `update settings response decoding`() {
        // Response{update_settings_response=5{can_delay_parameter_extractions=3=true}}
        val inner = byteArrayOf((3 shl 3).toByte(), 0x01)
        val out = ByteArrayOutputStream()
        val stream = CodedOutputStream.newInstance(out)
        stream.writeByteArray(5, inner)
        stream.flush()
        val decoded = ComposeInspectionCodec.decodeResponse(out.toByteArray())
        assertEquals(ComposeResponse.UpdateSettings(canDelayParameterExtractions = true), decoded)
    }

    @Test
    fun `get all parameters command exact bytes`() {
        // Command{get_all_parameters_command{root_view_id=5, skip_system_composables=true}}
        // field 3 length-delimited -> tag (3<<3)|2 = 0x1a; inner: 08 05 (int64) 10 01 (bool)
        assertEquals(
            "1a0408051001",
            hex(ComposeInspectionCodec.encodeGetAllParametersCommand(rootViewId = 5)),
        )
        // proto3 defaults omitted
        assertEquals(
            "1a020805",
            hex(
                ComposeInspectionCodec.encodeGetAllParametersCommand(
                    rootViewId = 5, skipSystemComposables = false
                )
            ),
        )
    }

    @Test
    fun `get all parameters response round trip`() {
        val stringParam = message {
            writeInt32(1, 1) // type STRING
            writeInt32(2, 2) // name -> string id 2 ("Hello")
        }
        val textParam = message {
            writeInt32(1, 14) // type ITERABLE
            writeInt32(2, 1) // name -> string id 1 ("Text")
            writeByteArray(3, stringParam) // elements
        }
        val group = message {
            writeSInt64(1, 7) // composable_id (zigzag -> 0x0e)
            writeByteArray(4, textParam) // merged_semantics
        }
        val response = message {
            writeInt64(1, 42) // root_view_id
            writeByteArray(2, message { // StringEntry strings (field 2 here, unlike GetComposables)
                writeInt32(1, 1)
                writeString(2, "Text")
            })
            writeByteArray(2, message {
                writeInt32(1, 2)
                writeString(2, "Hello")
            })
            writeByteArray(3, group) // parameter_groups
        }
        val decoded = ComposeInspectionCodec.decodeResponse(message { writeByteArray(3, response) })
        assertEquals(
            ComposeResponse.GetAllParameters(
                rootViewId = 42,
                groups = listOf(
                    ParameterGroup(
                        composableId = 7,
                        parameter = emptyList(),
                        mergedSemantics = listOf(
                            ComposeParameter(14, "Text", listOf(ComposeParameter(1, "Hello", emptyList()))),
                        ),
                        unmergedSemantics = emptyList(),
                    ),
                ),
            ),
            decoded,
        )
    }

    /** Helper mirroring the codec's own nesting: body of a length-delimited field. */
    private fun message(write: CodedOutputStream.() -> Unit): ByteArray {
        val out = ByteArrayOutputStream()
        val stream = CodedOutputStream.newInstance(out)
        stream.write()
        stream.flush()
        return out.toByteArray()
    }
}
