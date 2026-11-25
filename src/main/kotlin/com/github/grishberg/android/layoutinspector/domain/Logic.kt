package com.github.grishberg.android.layoutinspector.domain

import com.android.layoutinspector.LayoutInspectorResult
import com.android.layoutinspector.common.AppLogger
import com.android.layoutinspector.model.ClientWindow
import com.android.layoutinspector.model.LayoutFileData
import com.github.grishberg.android.layoutinspector.common.CoroutinesDispatchers
import com.github.grishberg.android.layoutinspector.compose.ComposeCaptureException
import com.github.grishberg.android.layoutinspector.compose.ComposeTreeMerger
import com.github.grishberg.android.layoutinspector.process.LayoutFileSystem
import com.github.grishberg.android.layoutinspector.process.LayoutInspectorCaptureTask
import com.github.grishberg.android.layoutinspector.process.RecordingConfig
import com.github.grishberg.android.layoutinspector.process.providers.ScreenSizeProvider
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

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

    private val screenSizeProvider = ScreenSizeProvider(dispatchers)
    private var recordingJob: Job? = null

    /** Background Jetpack Compose capture phase of the last displayed capture (see [startComposePhase]). */
    private var composeJob: Job? = null
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
        composeJob?.cancel()
        output.hideComposeProgress()
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
        logger.d("[0;36m$TAG: density = $density[0m")

        val screenSize = screenSizeProvider.getScreenSize(recordOptions.device)
        logger.d("[0;36m$TAG: screen size = $screenSize[0m")

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
        composeJob?.cancel()
        output.hideComposeProgress()
        previousRecordingConfig?.let { config ->

            recordingJob = coroutineScope.launch(errorHandler) {

                output.showLoading()

                val windows = coroutineScope.async(dispatchers.worker) {
                    return@async clientWindowsInput.getClientWindows(config.recordOptions)
                }
                val windowList: List<ClientWindow> = windows.await()
                if (windowList.any { it.displayName == config.clientWindow.displayName }) {
                    captureLayouts(config, isRefresh = true)
                } else {
                    recodLayoutFromNewDevice()
                }
            }
        }
    }

    private suspend fun captureLayouts(
        config: RecordingConfig, isRefresh: Boolean = false
    ) {
        val task = LayoutInspectorCaptureTask(layoutFileSystem, coroutineScope, logger, dispatchers)

        val liResult = task.captureViews(config)

        output.hideLoading()

        if (liResult.data == null || liResult.root == null) {
            output.showError(liResult.error)
            return
        }

        onSuccessCaptured(config, liResult, isRefresh)
        // A non-fatal error is shown after the result: the user gets both the tree and the reason.
        if (liResult.error.isNotEmpty()) {
            output.showError(liResult.error)
        }
        // The view tree is on screen already; the slow Jetpack Compose phase continues in the
        // background so the user can browse while it runs.
        if (config.recordOptions.composeModeEnabled) {
            startComposePhase(task, config, liResult)
        }
    }

    /**
     * Background phase of a compose-enabled capture: [LayoutInspectorCaptureTask.captureCompose]
     * runs on the worker dispatcher while its stages are painted into the window status bar;
     * when the compose roots arrive they are grafted into the *already displayed* tree (the merge
     * mutates node objects in place, so node identity — and with it the user's tree selection —
     * survives the in-place refresh). Grafted children are in memory only; the .li file saved in
     * phase one is view-only, like the uiautomator dump grafts.
     */
    private fun startComposePhase(
        task: LayoutInspectorCaptureTask,
        config: RecordingConfig,
        viewResult: LayoutInspectorResult,
    ) {
        composeJob?.cancel()
        composeJob = coroutineScope.launch(errorHandler) {
            output.showComposeProgress("Preparing...")
            try {
                // The stage callback fires on worker threads; `launch` here is scoped to *this*
                // job, so a cancelled phase can never paint stale progress after a new one began.
                val composeResult = task.captureCompose(config) { stage ->
                    launch(dispatchers.ui) {
                        output.showComposeProgress(stage)
                    }
                }
                val root = viewResult.root ?: return@launch
                val merged = ComposeTreeMerger(logger)
                    .mergeNodes(root, composeResult.roots, composeResult.semanticTexts)
                logger.d("$TAG: compose capture done, ${composeResult.roots.size} root(s) merged")
                if (composeResult.roots.isNotEmpty()) {
                    val mergedResult = LayoutInspectorResult(
                        merged,
                        viewResult.previewImage,
                        viewResult.data,
                        viewResult.options,
                        viewResult.error,
                    )
                    // isRefresh = true: keep the user's camera/zoom on the in-place update.
                    output.showResult(
                        LayoutFileData.fromLayoutInspectorResult(mergedResult),
                        config.recordOptions.label,
                        isRefresh = true,
                    )
                }
            } catch (t: ComposeCaptureException) {
                logger.e(t.message.orEmpty(), t)
                output.showError(t.message ?: "Jetpack Compose capture failed")
            } finally {
                // Runs on cancellation too (the finally block has no suspension point, so it
                // completes even though the job is cancelled).
                output.hideComposeProgress()
            }
        }
    }

    private fun onSuccessCaptured(
        config: RecordingConfig, liResult: LayoutInspectorResult, isRefresh: Boolean = false
    ) {
        val dpPerPixels = config.dpPerPixels
        val fileNamePrefix = config.recordOptions.fileNamePrefix
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
        output.showResult(LayoutFileData.fromLayoutInspectorResult(liResult), config.recordOptions.label, isRefresh)
        isOpenedLayout = true
    }

    fun openFile() {
        val file = dialogsInput.showOpenFileDialogAndReturnResult() ?: return
        // A background compose phase of the previous capture would graft into (and re-display)
        // the old tree over the freshly opened file.
        cancelRunningJobs()
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
        cancelRunningJobs()
    }

    /**
     * Cancels everything in flight (capture and/or background compose phase) and clears their
     * indicators. Called when the loading dialog is closed and when the window is destroyed:
     * after the view tree is displayed there is no modal dialog anymore, so these are the only
     * places a still-running compose capture can be stopped from.
     */
    fun cancelRunningJobs() {
        recordingJob?.cancel()
        composeJob?.cancel()
        recordingJob = null
        composeJob = null
        output.hideLoading()
        output.hideComposeProgress()
    }

    fun hasOpenedLayouts() = isOpenedLayout
}
