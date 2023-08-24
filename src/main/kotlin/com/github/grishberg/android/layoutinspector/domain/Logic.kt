package com.github.grishberg.android.layoutinspector.domain

import com.android.layoutinspector.LayoutInspectorResult
import com.android.layoutinspector.common.AppLogger
import com.android.layoutinspector.model.ClientWindow
import com.android.layoutinspector.model.LayoutFileData
import com.github.grishberg.android.layoutinspector.common.CoroutinesDispatchers
import com.github.grishberg.android.layoutinspector.process.LayoutFileSystem
import com.github.grishberg.android.layoutinspector.process.LayoutInspectorCaptureTask
import com.github.grishberg.android.layoutinspector.process.RecordingConfig
import com.github.grishberg.android.layoutinspector.process.providers.ScreenSizeProvider
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
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

    private var previousRecordingConfig: RecordingConfig? = null

    val canRefresh: Boolean
        get() = previousRecordingConfig != null

    fun startRecording() {
        recordingJob = coroutineScope.launch(errorHandler) {
            recodLayoutFromNewDevice()
        }
    }

    private suspend fun recodLayoutFromNewDevice() {
        val recordOptions = devicesInput.getLayoutOptions() ?: return

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


        val config = RecordingConfig(
            recordOptions.client,
            window,
            recordOptions.timeoutInSeconds,
            recordOptions.v2Enabled,
            dpPerPixels,
            recordOptions
        )
        previousRecordingConfig = config

        captureLayouts(config)
    }

    fun refreshLayout() {
        previousRecordingConfig?.let { config ->

            recordingJob = coroutineScope.launch(errorHandler) {

                output.showLoading()

                val windows = coroutineScope.async(dispatchers.worker) {
                    return@async clientWindowsInput.getClientWindows(config.recordOptions)
                }
                val windowList: List<ClientWindow> = windows.await()
                if (windowList.any { it.displayName == config.clientWindow.displayName }) {
                    captureLayouts(config)
                } else {
                    recodLayoutFromNewDevice()
                }
            }
        }
    }

    private suspend fun captureLayouts(
        config: RecordingConfig,
    ) {
        val task = LayoutInspectorCaptureTask(layoutFileSystem, coroutineScope, logger)

        val liResult = task.capture(config)

        output.hideLoading()

        if (liResult.data == null || liResult.root == null) {
            output.showError(liResult.error)
        } else {
            onSuccessCaptured(config.recordOptions.fileNamePrefix, liResult, config.dpPerPixels)
        }
    }

    private fun onSuccessCaptured(
        fileNamePrefix: String,
        liResult: LayoutInspectorResult,
        dpPerPixels: Double
    ) {
        val sdf = SimpleDateFormat("yyyyMMdd_HH-mm-ss.SSS")
        val formattedTime = sdf.format(Date())
        val liFileName = if (fileNamePrefix.isNotEmpty()) {
            "$fileNamePrefix-$formattedTime.li"
        } else {
            "layout-$formattedTime.li"
        }

        layoutFileSystem.saveLayoutToFile(liFileName, liResult.data!!)
        metaRepository.fileName = liFileName
        metaRepository.dpPerPixels = dpPerPixels
        metaRepository.serialize()

        logger.d("$TAG: Received result")
        output.showResult(LayoutFileData.fromLayoutInspectorResult(liResult))
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
