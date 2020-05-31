package com.android.layoutinspector

import java.io.File

object TestUtil {
    fun getTestFile(fn: String = ""): File {
        val classLoader = javaClass.classLoader
        return File(classLoader.getResource(fn)!!.file)
    }

}
