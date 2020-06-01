package com.github.grishberg.android.layoutinspector.domain

import com.android.layoutinspector.common.AppLogger
import com.android.layoutinspector.parser.LayoutFileDataParser
import com.github.grishberg.android.layoutinspector.process.LayoutFileSystem
import com.github.grishberg.android.layoutinspector.process.LayoutInspectorCaptureTask
import com.github.grishberg.android.layoutinspector.process.providers.ScreenSizeProvider
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import java.io.IOException

private const val TAG = "Logic"

class Logic(
    private val devicesInput: LayoutRecordOptionsInput,
    private val clientWindowsInput: ClientWindowsInput,
    private val output: LayoutResultOutput,
    private val logger: AppLogger,
    private val layoutFileSystem: LayoutFileSystem,
    private val dialogsInput: DialogsInput
) {
    private val screenSizeProvider = ScreenSizeProvider()

    private val errorHandler = CoroutineExceptionHandler { _, exception ->
        GlobalScope.launch(Dispatchers.Swing) {
            output.hideLoading()
            logger.e(exception.message.orEmpty(), exception)
            output.showError(exception.message.orEmpty())
        }
    }

    fun startRecording() {
        GlobalScope.launch(errorHandler) {
            val recordOptions = devicesInput.getLayoutOptions() ?: return@launch

            output.showLoading()

            val window = clientWindowsInput.getSelectedWindow(recordOptions)

            val density = recordOptions.device.density
            logger.d("$TAG: density = $density")

            val screenSize = screenSizeProvider.getScreenSize(recordOptions.device)
            logger.d("$TAG: screen size = $screenSize")

            val dpPerPixels = (density / 160.0)
            logger.d("$TAG: dp per pixels = $dpPerPixels")

            val task = LayoutInspectorCaptureTask()

            val liResult = task.capture(window, recordOptions.timeoutInSeconds)

            output.hideLoading()

            if (liResult.data == null) {
                output.showError(liResult.error)
            } else {
                onSuccessCaptured(liResult.data, dpPerPixels)
            }
        }
    }

    private fun onSuccessCaptured(data: ByteArray, dpPerPixels: Double) {
        layoutFileSystem.saveLayoutToFile(data)
        logger.d("$TAG: Received result, parsing...")
        val capture = LayoutFileDataParser.parseFromBytes(data)
        logger.d("$TAG: Parsing is ended.")

        capture.dpPerPixels = dpPerPixels
        output.showResult(capture)
    }

    fun openFile() {
        val file = dialogsInput.showOpenFileDialogAndReturnResult() ?: return
        try {
            val capture = LayoutFileDataParser.parseFromFile(file)
            output.showResult(capture)
        } catch (e: IOException) {
            output.showError(e.message.toString())
        }
    }
}
