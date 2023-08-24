package com.github.grishberg.android.layoutinspector.process

import com.android.ddmlib.CollectingOutputReceiver
import com.android.ddmlib.IDevice
import com.android.ddmlib.IShellOutputReceiver
import java.io.BufferedReader
import java.io.File
import java.util.concurrent.TimeUnit

class HierarchyDump(
    private val device: IDevice,
    private val layoutFileSystem: LayoutFileSystem,
) {

    private val PATTERN = "[^ ]+.xml".toRegex()


    fun getHierarchyDump(): String? {
        val receiver = CollectingOutputReceiver()
        device.executeShellCommand("uiautomator dump", receiver)
        receiver.awaitCompletion(60L, TimeUnit.SECONDS)
        val output = receiver.output ?: return null
        val matchResult = PATTERN.find(output)?: return null
        val localFile =  uploadDumpFile(matchResult.value)

        return readStringFile(localFile)
    }


    private fun uploadDumpFile(fileName: String): File {
        if (!layoutFileSystem.dumpsDir.exists()) {
            layoutFileSystem.dumpsDir.mkdirs()
        }
        val localFile = File(layoutFileSystem.dumpsDir, "window_dump.xml")
        device.pullFile(fileName, localFile.absolutePath)
        return localFile
    }

    private fun readStringFile(file: File): String {
        val bufferedReader: BufferedReader = file.bufferedReader()
        return bufferedReader.use { it.readText() }
    }
}
