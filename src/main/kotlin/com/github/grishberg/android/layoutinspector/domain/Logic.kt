package com.github.grishberg.android.layoutinspector.domain

import com.android.layoutinspector.common.AppLogger
import com.github.grishberg.android.layoutinspector.common.CoroutinesDispatchers
import com.github.grishberg.android.layoutinspector.process.LayoutFileSystem
import com.github.grishberg.android.layoutinspector.process.LayoutInspectorCaptureTask
import com.github.grishberg.android.layoutinspector.process.providers.ScreenSizeProvider
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

private const val TAG = "Logic"

class Logic(
    private val devicesInput: LayoutRecordOptionsInput,
    private val windowsListInput: WindowsListInput,
    private val clientWindowsInput: ClientWindowsInput,
    private val layoutParserInput: LayoutParserInput,
    private val output: LayoutResultOutput,
    private val logger: AppLogger,
    private val layoutFileSystem: LayoutFileSystem,
    private val dialogsInput: DialogsInput,
    private val metaRepository: MetaRepository,
    private val coroutineScope: CoroutineScope,
    private val dispatchers: CoroutinesDispatchers
) {
    private val screenSizeProvider = ScreenSizeProvider()
    private var recordingJob: Job? = null
    private var isOpenedLayout = false

    private val errorHandler = CoroutineExceptionHandler { _, exception ->
        coroutineScope.launch(dispatchers.ui) {
            output.hideLoading()
            logger.e(exception.message.orEmpty(), exception)
            output.showError(exception.message.orEmpty())
        }
    }

    fun startRecording() {
        recordingJob = coroutineScope.launch(errorHandler) {
            val recordOptions = devicesInput.getLayoutOptions() ?: return@launch

            output.showLoading()

            val windows = coroutineScope.async(dispatchers.worker) {
                return@async clientWindowsInput.getClientWindows(recordOptions)
            }
            val windowList = windows.await()

            if (windowList.isEmpty()) {
                throw IllegalStateException("No windows for client")
            }

            val window = if (windowList.size == 1) {
                windowList[0]
            } else {
                output.hideLoading()
                val selectedWindow = windowsListInput.getSelectedWindow(windowList)
                output.showLoading()
                selectedWindow
            }

            val density = recordOptions.device.density
            logger.d("$TAG: density = $density")

            val screenSize = screenSizeProvider.getScreenSize(recordOptions.device)
            logger.d("$TAG: screen size = $screenSize")

            val dpPerPixels = (density / 160.0)
            logger.d("$TAG: dp per pixels = $dpPerPixels")

            val task = LayoutInspectorCaptureTask(logger)

            val liResult = task.capture(window, recordOptions.timeoutInSeconds)

            output.hideLoading()

            if (liResult.data == null) {
                output.showError(liResult.error)
            } else {
                onSuccessCaptured(recordOptions.fileNamePrefix, liResult.data, dpPerPixels)
            }
        }
    }

    private fun onSuccessCaptured(fileNamePrefix: String, data: ByteArray, dpPerPixels: Double) {
        val sdf = SimpleDateFormat("yyyyMMdd_HH-mm-ss.SSS")
        val formattedTime = sdf.format(Date())
        val liFileName = if (fileNamePrefix.isNotEmpty()) {
            "$fileNamePrefix-$formattedTime.li"
        } else {
            "layout-$formattedTime.li"
        }

        layoutFileSystem.saveLayoutToFile(liFileName, data)
        metaRepository.fileName = liFileName
        metaRepository.dpPerPixels = dpPerPixels
        metaRepository.serialize()

        logger.d("$TAG: Received result, parsing...")
        val capture = layoutParserInput.parseFromBytes(data)
        logger.d("$TAG: Parsing is ended.")

        output.showResult(capture)
        isOpenedLayout = true
    }

    fun openFile() {
        val file = dialogsInput.showOpenFileDialogAndReturnResult() ?: return
        try {
            val capture = layoutParserInput.parseFromFile(file)
            metaRepository.restoreForFile(file.name, capture.node)

            output.showResult(capture)
            isOpenedLayout = true
        } catch (e: IOException) {
            output.showError(e.message.toString())
        }
    }

    fun onLoadingDialogClosed() {
        recordingJob?.cancel()
        output.hideLoading()
        recordingJob = null
    }

    fun hasOpenedLayouts() = isOpenedLayout
}
