package com.android.layoutinspector.parser

import com.android.layoutinspector.model.ViewNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * The V1 stream encodes every property value as `<byteLength>:<utf8Bytes>`. Values with
 * multi-byte UTF-8 (e.g. Cyrillic `mText`) used to shift all following char offsets and crash
 * the parser with StringIndexOutOfBoundsException.
 */
class ViewNodeParserTest {

    private fun deviceLine(indent: Int, name: String, vararg properties: Pair<String, String>): String {
        // real V1 stream format per property: `name=<byteLength>,<utf8Value>` joined by `:`
        val props = properties.joinToString(":") { (key, value) ->
            "$key=${value.toByteArray(Charsets.UTF_8).size},$value"
        }
        return " ".repeat(indent) + name + "@" + Integer.toHexString(1715004) + " " + props + "\n"
    }

    @Test
    fun parseHandlesMultiByteUtf8PropertyValues() {
        val stream = deviceLine(
            0,
            "android.widget.FrameLayout",
            "text" to "Привет мир",
            "id" to "com.test:id/title",
        ) + deviceLine(
            4,
            "android.widget.TextView",
            "text" to "Х",
            "mViewFlags" to "0x1",
        ) + "DONE."

        val root = ViewNodeParser.parse(stream.toByteArray(Charsets.UTF_8))

        assertNotNull(root)
        assertEquals("android.widget.FrameLayout", root!!.name)
        assertEquals("Привет мир", root.getProperty("text")?.value)
        // the property AFTER the multi-byte value must still be aligned:
        assertEquals("com.test:id/title", root.getProperty("id")?.value)

        val child = root.children.single() as ViewNode
        assertEquals("Х", child.getProperty("text")?.value)
        assertEquals("0x1", child.getProperty("mViewFlags")?.value)
    }

    @Test
    fun parseStillHandlesAsciiValues() {
        val stream = deviceLine(0, "android.view.View", "text" to "hello", "tag" to "ok") + "DONE."

        val root = ViewNodeParser.parse(stream.toByteArray(Charsets.UTF_8))!!

        assertEquals("hello", root.getProperty("text")?.value)
        assertEquals("ok", root.getProperty("tag")?.value)
    }
}
