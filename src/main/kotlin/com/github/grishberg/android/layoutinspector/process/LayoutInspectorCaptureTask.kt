package com.github.grishberg.android.layoutinspector.process

import com.android.layoutinspector.LayoutInspectorBridge
import com.android.layoutinspector.LayoutInspectorBridge.V2_MIN_API
import com.android.layoutinspector.LayoutInspectorCaptureOptions
import com.android.layoutinspector.LayoutInspectorResult
import com.android.layoutinspector.ProtocolVersion
import com.android.layoutinspector.common.AppLogger
import com.github.grishberg.android.layoutinspector.domain.DumpViewNode
import com.github.grishberg.android.layoutinspector.domain.RecordingMode
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

        //TODO: place under condition
        val layoutResultAsync = scope.async(Dispatchers.IO) {
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
        val shouldLoadDump = recordingConfig.recordOptions.recordingMode == RecordingMode.LayoutsAndComposeDump ||
            recordingConfig.recordOptions.recordingMode == RecordingMode.LayoutsAndDump ||
            recordingConfig.recordOptions.recordingMode == RecordingMode.Dump

        val dumpNodeAsyncResult: DumpViewNode? = if (shouldLoadDump) {
            val viewDumpsAsync = scope.async(Dispatchers.IO) {
                return@async getViewDumps(recordingConfig)
            }
            viewDumpsAsync.await()
        } else {
            null
        }

        val shouldLoadLayouts = recordingConfig.recordOptions.recordingMode == RecordingMode.Layouts ||
            recordingConfig.recordOptions.recordingMode == RecordingMode.LayoutsAndDump ||
            recordingConfig.recordOptions.recordingMode == RecordingMode.LayoutsAndComposeDump

        val layoutAsyncResult: LayoutInspectorResult? = if (shouldLoadLayouts) {
            layoutResultAsync.await()
        } else {
            null
        }

        val result = getResult(layoutAsyncResult, recordingConfig, dumpNodeAsyncResult)

        logger.d("$TAG: capturing is done, error: ${result.error}")
        return result
    }

    private fun getResult(
        layoutAsyncResult: LayoutInspectorResult?, recordingConfig: RecordingConfig, dumpNodeAsyncResult: DumpViewNode?
    ): LayoutInspectorResult {
        val layoutRootNode = layoutAsyncResult?.root
        return when(recordingConfig.recordOptions.recordingMode) {
            RecordingMode.Layouts ->layoutAsyncResult ?: throw IllegalStateException()

            RecordingMode.LayoutsAndComposeDump -> {
                val result = if (dumpNodeAsyncResult != null && layoutRootNode != null) {

                    val treeMerger = TreeMerger()

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
                result ?: throw IllegalStateException()
            }
            RecordingMode.LayoutsAndDump -> {
                if(dumpNodeAsyncResult == null) {
                    layoutAsyncResult ?: throw IllegalStateException()
                } else {
                    LayoutInspectorResult(
                        CompoundRootNode(layoutRootNode!!, dumpNodeAsyncResult),
                        layoutAsyncResult.previewImage,
                        layoutAsyncResult.data,
                        layoutAsyncResult.options,
                        layoutAsyncResult.error
                    )
                }
            }
            RecordingMode.Dump -> {
                LayoutInspectorResult(
                    dumpNodeAsyncResult,
                    layoutAsyncResult?.previewImage,
                    layoutAsyncResult?.data,
                    layoutAsyncResult?.options,
                    layoutAsyncResult?.error ?: ""
                 )
            }
        }
    }

    private fun getViewDumps(recordingConfig: RecordingConfig): DumpViewNode? {
        logger.d("$TAG: start capture view dumps")
        val dumper = HierarchyDump(recordingConfig.client.device, layoutFileSystem)
        val dumpString = dumper.getHierarchyDump() ?: return null

        val dumpParser = HierarchyDumpParser()
        return dumpParser.parseDump(dumpString)
    }

    private fun determineProtocolVersion(apiVersion: Int, v2Enabled: Boolean): ProtocolVersion {
        return if (apiVersion >= V2_MIN_API && v2Enabled) ProtocolVersion.Version2 else ProtocolVersion.Version1
    }
}
