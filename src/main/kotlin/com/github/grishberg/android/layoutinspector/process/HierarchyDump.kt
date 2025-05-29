package com.github.grishberg.android.layoutinspector.process

import com.android.ddmlib.CollectingOutputReceiver
import com.android.ddmlib.IDevice
import com.android.ddmlib.IShellOutputReceiver
import com.android.layoutinspector.common.AppLogger
import java.io.BufferedReader
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.runBlocking
import com.github.grishberg.android.layoutinspector.common.CoroutinesDispatchers

private const val TAG = "HierarchyDump"

class HierarchyDump(
    private val device: IDevice,
    private val layoutFileSystem: LayoutFileSystem,
    private val logger: AppLogger,
    private val dispatchers: CoroutinesDispatchers,
) {

    private val PATTERN = "UI\\shierchary\\sdumped\\sto:\\s([^ ]+.xml)".toRegex()


    suspend fun getHierarchyDump(): String? {
        return withContext(dispatchers.worker) {
            val receiver = CollectingOutputReceiver()
            device.executeShellCommand("uiautomator dump", receiver)
            receiver.awaitCompletion(60L, TimeUnit.SECONDS)
            logger.d("$TAG: getViewDumps() receiver.output: ${receiver.output}")
            val output = receiver.output ?: return@withContext null
            val matchResult = PATTERN.find(output)?: return@withContext null

            logger.d("$TAG: getViewDumps() output: $output")
            val localFile =  uploadDumpFile(matchResult.groupValues[1])

            logger.d("$TAG: getViewDumps() start reading: ${localFile.name}")
            return@withContext readStringFile(localFile)
        }
    }


    private suspend fun uploadDumpFile(fileName: String): File {
        if (!layoutFileSystem.dumpsDir.exists()) {
            layoutFileSystem.dumpsDir.mkdirs()
        }
        val localFile = File(layoutFileSystem.dumpsDir, "window_dump.xml")
        withContext(dispatchers.worker) {
            device.pullFile(fileName, localFile.absolutePath)
        }
        return localFile
    }

    private fun readStringFile(file: File): String {
        val bufferedReader: BufferedReader = file.bufferedReader()
        return bufferedReader.use { it.readText() }
    }
}
