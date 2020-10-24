package com.github.grishberg.android.layoutinspector.process

import com.android.layoutinspector.common.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream

private const val TAG = "FileSystem"
const val LAYOUTS_DIR = "layouts"

class LayoutFileSystem(
    private val logger: AppLogger,
    baseDir: File
) {
    val layoutDir = File(baseDir, LAYOUTS_DIR)

    init {
        if (!layoutDir.exists()) {
            layoutDir.mkdirs()
        }
    }

    fun saveLayoutToFile(fileName: String, data: ByteArray) {
        GlobalScope.launch(Dispatchers.IO) {
            saveToFile(fileName, data)
        }
    }

    private fun saveToFile(fileName: String, data: ByteArray) {
        val dir = layoutDir
        if (!dir.exists()) {
            dir.mkdirs()
        }

        var bs: BufferedOutputStream? = null
        val file = File(dir, fileName)

        try {
            val fs = FileOutputStream(file)
            bs = BufferedOutputStream(fs)
            bs.write(data)
            bs.close()
            bs = null
        } catch (e: java.lang.Exception) {
            logger.e("$TAG: save trace file failed", e)
            e.printStackTrace()
        }

        if (bs != null) try {
            bs.close()
        } catch (e: Exception) {
        }
    }
}
