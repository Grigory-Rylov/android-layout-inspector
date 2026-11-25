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

    @Test
    fun parseRecoversFromRawCarriageReturnInsideValue() {
        // The device escapes only '\n'; a raw '\r' inside a value splits the stream line and is
        // NOT marked - the parser must re-join the fragment (the crash from the 17:29 log).
        val value = "Первый\rвторой"
        val props = "text=${value.toByteArray(Charsets.UTF_8).size},$value" +
            ":id=${"com.test:id/title".toByteArray(Charsets.UTF_8).size},com.test:id/title"
        val stream = "  com.foo.View@abc $props\nDONE."

        val root = ViewNodeParser.parse(stream.toByteArray(Charsets.UTF_8))!!

        assertEquals("com.foo.View", root.name)
        // the separator byte is restored when re-joining (CR -> LF, same length)
        assertEquals("Первый\nвторой", root.getProperty("text")?.value)
        assertEquals("com.test:id/title", root.getProperty("id")?.value)
    }

    @Test
    fun parseSkipsUndiagnosableGarbageInsteadOfFailing() {
        val stream = deviceLine(0, "android.view.View", "text" to "ok") +
            "!!! binary garbage without at sign @@ at all\n" +
            deviceLine(4, "android.widget.Button", "text" to "Btn") +
            "DONE."

        val root = ViewNodeParser.parse(stream.toByteArray(Charsets.UTF_8))

        assertNotNull(root)
    }
}
