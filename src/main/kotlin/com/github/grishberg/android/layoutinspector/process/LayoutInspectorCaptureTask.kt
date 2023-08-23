package com.github.grishberg.android.layoutinspector.process

import com.android.layoutinspector.LayoutInspectorBridge
import com.android.layoutinspector.LayoutInspectorBridge.V2_MIN_API
import com.android.layoutinspector.LayoutInspectorCaptureOptions
import com.android.layoutinspector.LayoutInspectorResult
import com.android.layoutinspector.ProtocolVersion
import com.android.layoutinspector.common.AppLogger
import com.github.grishberg.android.layoutinspector.domain.AbstractViewNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

private const val TAG = "LayoutInspectorCaptureTask"

class LayoutInspectorCaptureTask(
    private val layoutFileSystem: LayoutFileSystem,
    private val scope: CoroutineScope,
    private val logger: AppLogger,
) {

    suspend fun capture(recordingConfig: RecordingConfig): LayoutInspectorResult {

        val layoutResultAsync = scope.async(Dispatchers.IO) {
            logger.d("$TAG: start capture view timeout = ${recordingConfig.timeoutInSeconds}")

            try {
                val version: ProtocolVersion = determineProtocolVersion(
                    recordingConfig.client.device.version.apiLevel, recordingConfig.v2Enabled
                )
                val options = LayoutInspectorCaptureOptions()
                options.version = version

                val captureView = LayoutInspectorBridge.captureView(
                    logger, recordingConfig.clientWindow, options, recordingConfig.timeoutInSeconds.toLong()
                )

                return@async captureView
            } catch (e: Exception) {
                logger.e(TAG, e)
                throw e
            }
        }
        val viewDumpsAsync = scope.async(Dispatchers.IO) {
            return@async getViewDumps(recordingConfig)
        }

        val layoutAsyncResult = layoutResultAsync.await()
        val dumpNodeAsyncResult = viewDumpsAsync.await()

        val result =
        if (recordingConfig.recordOptions.dumpViewModeEnabled && dumpNodeAsyncResult != null) {
            LayoutInspectorResult(dumpNodeAsyncResult, dumpNodeAsyncResult, layoutAsyncResult.previewImage, layoutAsyncResult.data, layoutAsyncResult.options, layoutAsyncResult.error)
        } else {
            layoutAsyncResult
        }

        logger.d("$TAG: capturing is done, error: ${result.error}")
        return result
    }

    private fun getViewDumps(recordingConfig: RecordingConfig): AbstractViewNode? {
        if (!recordingConfig.recordOptions.dumpViewModeEnabled) {
            return null
        }
        logger.d("$TAG: start capture view dumps")
        val dumper = HierarchyDump(recordingConfig.client.device, layoutFileSystem)
        val dumpString = dumper.getHierarchyDump() ?: return null

        val dumpParser = HierarchyDumpParser()
        val dumpRootNode = dumpParser.parseDump(dumpString)
        return dumpRootNode

    }

    private fun determineProtocolVersion(apiVersion: Int, v2Enabled: Boolean): ProtocolVersion {
        return if (apiVersion >= V2_MIN_API && v2Enabled) ProtocolVersion.Version2 else ProtocolVersion.Version1
    }
}
