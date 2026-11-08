package com.github.grishberg.android.layoutinspector.compose

import com.github.grishberg.android.layoutinspector.compose.ComposeInspectionCodec.ComposeParameter
import com.github.grishberg.android.layoutinspector.compose.ComposeInspectionCodec.ParameterGroup
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import org.junit.Test

/**
 * Semantics -> text resolution of [ComposeTextExtractor]: merged/unmerged preference, ITERABLE
 * flattening and the shapes the compose agent actually serializes for the `Text` property.
 */
class ComposeTextExtractorTest {

    private fun stringParam(value: String) =
        ComposeParameter(ComposeTextExtractor.TYPE_STRING, value, emptyList())

    private fun textParam(value: String) = ComposeParameter(
        ComposeTextExtractor.TYPE_ITERABLE,
        ComposeTextExtractor.TEXT_SEMANTIC_NAME,
        listOf(stringParam(value)),
    )

    private fun group(id: Long, merged: List<ComposeParameter>, unmerged: List<ComposeParameter> = emptyList()) =
        ParameterGroup(id, emptyList(), merged, unmerged)

    @Test
    fun `merged Text semantics becomes the composable text`() {
        assertEquals(
            mapOf(7L to "Hello"),
            ComposeTextExtractor.extract(listOf(group(7, listOf(textParam("Hello"))))),
        )
    }

    @Test
    fun `unmerged semantics is the fallback`() {
        assertEquals("Fallback", ComposeTextExtractor.textOf(group(3, emptyList(), listOf(textParam("Fallback")))))
    }

    @Test
    fun `merged wins over unmerged`() {
        val group = group(1, listOf(textParam("Merged")), listOf(textParam("Unmerged")))
        assertEquals("Merged", ComposeTextExtractor.textOf(group))
    }

    @Test
    fun `iterable elements concatenate`() {
        val param = ComposeParameter(
            ComposeTextExtractor.TYPE_ITERABLE,
            ComposeTextExtractor.TEXT_SEMANTIC_NAME,
            listOf(stringParam("Hello, "), stringParam("world")),
        )
        assertEquals("Hello, world", ComposeTextExtractor.textOf(group(9, listOf(param))))
    }

    @Test
    fun `nested iterables flatten`() {
        val inner = ComposeParameter(
            ComposeTextExtractor.TYPE_ITERABLE, "", listOf(stringParam("Deep"))
        )
        val param = ComposeParameter(ComposeTextExtractor.TYPE_ITERABLE, "Text", listOf(inner))
        assertEquals("Deep", ComposeTextExtractor.textOf(group(9, listOf(param))))
    }

    @Test
    fun `groups without Text semantics are skipped`() {
        val group = group(
            5,
            listOf(ComposeParameter(ComposeTextExtractor.TYPE_ITERABLE, "SetTextOrder", listOf(stringParam("x")))),
        )
        assertNull(ComposeTextExtractor.textOf(group))
        assertEquals(emptyMap<Long, String>(), ComposeTextExtractor.extract(listOf(group)))
    }

    @Test
    fun `empty iterable text produces no entry`() {
        val empty = ComposeParameter(ComposeTextExtractor.TYPE_ITERABLE, "Text", emptyList())
        assertEquals(emptyMap<Long, String>(), ComposeTextExtractor.extract(listOf(group(2, listOf(empty)))))
    }

    @Test
    fun `string typed Text property yields nothing`() {
        // For a property parameter `name` is the property key, not a value: a STRING-typed
        // "Text" carries no string and must not label the node with the word "Text".
        val group = group(
            4,
            listOf(ComposeParameter(ComposeTextExtractor.TYPE_STRING, ComposeTextExtractor.TEXT_SEMANTIC_NAME, emptyList())),
        )
        assertNull(ComposeTextExtractor.textOf(group))
    }
}
