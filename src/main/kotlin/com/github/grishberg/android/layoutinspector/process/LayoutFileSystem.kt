package com.github.grishberg.android.layoutinspector.process

import com.android.layoutinspector.common.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "FileSystem"

class LayoutFileSystem(
    private val logger: AppLogger
) {
    private val layoutDir = "layouts"

    fun saveLayoutToFile(data: ByteArray) {
        GlobalScope.launch(Dispatchers.IO) {
            saveToFile(data)
        }
    }

    private fun saveToFile(data: ByteArray) {
        val dir = File(layoutDir)
        if (!dir.exists()) {
            dir.mkdir()
        }
        val sdf = SimpleDateFormat("yyyyMMdd_HH-mm-ss.SSS")
        val formattedTime = sdf.format(Date())
        val fn = "layout-$formattedTime.li"

        var bs: BufferedOutputStream? = null
        val file = File(dir, fn)

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