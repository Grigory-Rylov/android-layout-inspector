package com.github.grishberg.android.layoutinspector.process

import java.io.BufferedReader
import java.io.File
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import org.junit.Test

class HierarchyDumpParserTest {


    private val underTest = HierarchyDumpParser()

    @Test
    fun test() {
        val result = underTest.parseDump(readFile(getDumpFile()))

        assertNotNull(result)

        assertEquals(0, result!!.locationOnScreenX)
        assertEquals(0, result.locationOnScreenY)
        assertEquals(1080, result.width)
        assertEquals(1920, result.height)

        val firstChild = result.children.first()
        assertNotNull(firstChild)

        val content = firstChild.children.first()
        assertNotNull(content)

        assertEquals("android:id/content", content.id)
        assertEquals(0, content.locationOnScreenX)
        assertEquals(1080, content.width)
        assertEquals(63, content.locationOnScreenY)
        assertEquals(1794, content.height)

        val frameLayout = content.children.first()
        assertNotNull(frameLayout)
        assertEquals("android.widget.FrameLayout", frameLayout.name)
    }

    private fun readFile(file: File): String {
        val bufferedReader: BufferedReader = file.bufferedReader()
        return bufferedReader.use { it.readText() }
    }

    private fun getDumpFile(): File {
        var classLoader = javaClass.classLoader
        val filePath = classLoader.getResource("dump.xml")?.file ?: throw IllegalStateException()
        return File(filePath)
    }
}
