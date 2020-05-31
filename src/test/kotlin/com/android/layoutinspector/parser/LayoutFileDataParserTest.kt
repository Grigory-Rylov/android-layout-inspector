package com.android.layoutinspector.parser

import com.android.layoutinspector.TestUtil.getTestFile
import com.android.layoutinspector.model.LayoutFileData
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class LayoutFileDataParserTest {
    private lateinit var myTestData: LayoutFileData

    @Before
    fun setUp() {
        val file = getTestFile()
        myTestData = LayoutFileDataParser.parseFromFile(file)
    }

    @Test
    fun testReadingFile() {
        Assert.assertNotNull(myTestData)
    }
}