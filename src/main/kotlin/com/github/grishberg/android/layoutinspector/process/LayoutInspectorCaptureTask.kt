package com.github.grishberg.android.layoutinspector.process

import com.android.layoutinspector.LayoutInspectorBridge
import com.android.layoutinspector.LayoutInspectorBridge.V2_MIN_API
import com.android.layoutinspector.LayoutInspectorCaptureOptions
import com.android.layoutinspector.LayoutInspectorResult
import com.android.layoutinspector.ProtocolVersion
import com.android.layoutinspector.common.AppLogger
import com.github.grishberg.android.layoutinspector.compose.ComposeCaptureService
import com.github.grishberg.android.layoutinspector.domain.DumpViewNode
import com.github.grishberg.android.layoutinspector.common.CoroutinesDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

private const val TAG = "LayoutInspectorCaptureTask"

class LayoutInspectorCaptureTask(
    private val layoutFileSystem: LayoutFileSystem,
    private val scope: CoroutineScope,
    private val logger: AppLogger,
    private val dispatchers: CoroutinesDispatchers,
) {

    /**
     * Fast phase of a capture: JDWP view hierarchy (+ optional uiautomator dump merge).
     * The slow Jetpack Compose phase runs separately via [captureCompose] so the view tree can
     * be shown immediately; the returned result never carries compose grafts.
     */
    suspend fun captureViews(recordingConfig: RecordingConfig): LayoutInspectorResult {

        val layoutResultAsync = scope.async(dispatchers.worker) {
            logger.d("$TAG: start capture view timeout = ${recordingConfig.timeoutInSeconds}")

            try {
                val version: ProtocolVersion = determineProtocolVersion(
                    recordingConfig.client.device.version.apiLevel, recordingConfig.v2Enabled
                )
                val options = LayoutInspectorCaptureOptions()
                options.version = version

                return@async LayoutInspectorBridge.captureView(
                    logger, recordingConfig.clientWindow, options, recordingConfig.timeoutInSeconds.toLong()
                )
            } catch (e: Exception) {
                logger.e(TAG, e)
                throw e
            }
        }
        val layoutAsyncResult = layoutResultAsync.await()

        val dumpNodeAsyncResult: DumpViewNode? = if (recordingConfig.recordOptions.dumpViewModeEnabled) {
            val viewDumpsAsync = scope.async(dispatchers.worker) {
                return@async getViewDumps(recordingConfig)
            }
            viewDumpsAsync.await()
        } else {
            null
        }

        val layoutRootNode = layoutAsyncResult.root
        logger.d("$TAG: prepare capture : dumpViewModeEnabled = ${recordingConfig.recordOptions.dumpViewModeEnabled}, dumpResult != null : ${dumpNodeAsyncResult != null}, layoutRootNode != null: ${layoutRootNode != null}")
        val result =
            if (recordingConfig.recordOptions.dumpViewModeEnabled && dumpNodeAsyncResult != null && layoutRootNode != null) {
                logger.w("$TAG: found dump and layouts, try to merge")

                val treeMerger = TreeMerger(logger)

                LayoutInspectorResult(
                    treeMerger.mergeNodes(layoutRootNode, dumpNodeAsyncResult),
                    layoutAsyncResult.previewImage,
                    layoutAsyncResult.data,
                    layoutAsyncResult.options,
                    layoutAsyncResult.error
                )
            } else {
                layoutAsyncResult
            }

        logger.d("$TAG: view capturing is done, error: ${result.error}")
        return result
    }

    /**
     * Slow phase of a compose-enabled capture: App Inspection based [ComposeCaptureService]
     * running on the worker dispatcher. [onStage] receives human-readable phase updates and is
     * invoked on a worker thread (callers must marshal to the UI thread themselves).
     * Propagates [com.github.grishberg.android.layoutinspector.compose.ComposeCaptureException]
     * with a user-presentable message; the caller decides how to surface it.
     */
    suspend fun captureCompose(
        recordingConfig: RecordingConfig,
        onStage: (String) -> Unit,
    ): ComposeCaptureService.ComposeCaptureResult = withContext(dispatchers.worker) {
        // withContext (not scope.async) so the job is a child of the *caller*: cancelling the
        // compose phase (new capture / refresh / close) really stops this capture — otherwise an
        // orphaned run would keep the app's inspectors busy and stopInspectors the next capture.
        logger.d("$TAG: starting compose capture")
        ComposeCaptureService(logger).capture(
            deviceSerial = recordingConfig.client.device.serialNumber,
            pid = recordingConfig.client.clientData.pid,
            device = recordingConfig.client.device,
            packageName = recordingConfig.client.clientData.clientDescription,
            onStage = onStage,
        )
    }

    private suspend fun getViewDumps(recordingConfig: RecordingConfig): DumpViewNode? {
        logger.d("$TAG: getViewDumps()")
        val dumper = HierarchyDump(recordingConfig.client.device, layoutFileSystem, logger, dispatchers)
        val hierarchyDump = dumper.getHierarchyDump()
        logger.d("$TAG: getViewDumps() : hierarchyDump received ${hierarchyDump != null}")
        val dumpString = hierarchyDump ?: return null

        val dumpParser = HierarchyDumpParser()
        return dumpParser.parseDump(dumpString)
    }

    private fun determineProtocolVersion(apiVersion: Int, v2Enabled: Boolean): ProtocolVersion {
        return if (apiVersion >= V2_MIN_API && v2Enabled) ProtocolVersion.Version2 else ProtocolVersion.Version1
    }
}
