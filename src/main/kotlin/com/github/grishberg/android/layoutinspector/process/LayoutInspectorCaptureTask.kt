package com.github.grishberg.android.layoutinspector.process

import com.android.layoutinspector.LayoutInspectorBridge
import com.android.layoutinspector.LayoutInspectorBridge.V2_MIN_API
import com.android.layoutinspector.LayoutInspectorCaptureOptions
import com.android.layoutinspector.LayoutInspectorResult
import com.android.layoutinspector.ProtocolVersion
import com.android.layoutinspector.common.AppLogger
import com.github.grishberg.android.layoutinspector.domain.*
import com.github.grishberg.android.layoutinspector.settings.TreeSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import java.util.EnumSet

private const val TAG = "LayoutInspectorCaptureTask"

class LayoutInspectorCaptureTask(
    private val layoutFileSystem: LayoutFileSystem,
    private val scope: CoroutineScope,
    private val logger: AppLogger,
    private val treeSettings: TreeSettings,
) {
    private var composeClient: ComposeLayoutInspectorClient? = null

    suspend fun capture(recordingConfig: RecordingConfig): LayoutInspectorResult {
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
        val layoutAsyncResult = layoutResultAsync.await()

        val dumpNodeAsyncResult: DumpViewNode? = if (recordingConfig.recordOptions.dumpViewModeEnabled) {
            val viewDumpsAsync = scope.async(Dispatchers.IO) {
                return@async getViewDumps(recordingConfig)
            }
            viewDumpsAsync.await()
        } else {
            null
        }

        // Try to get Compose data if available
        val composeDataAsync = scope.async(Dispatchers.IO) {
            try {
                if (composeClient == null) {
                    composeClient = ComposeLayoutInspectorClient(
                        recordingConfig.client,
                        treeSettings,
                        ComposeInspectorMessenger(logger, recordingConfig.client),
                        EnumSet.noneOf(Capability::class.java),
                        object : InspectorClientLaunchMonitor {
                            override fun updateProgress(state: AttachErrorState) {
                                logger.d("$TAG: Compose inspector state changed to $state")
                            }
                        },
                        "1.2.1" // Minimum supported version
                    )
                }

                val composeResult = composeClient?.getComposables(
                    rootViewId = layoutAsyncResult.root?.id ?: 0,
                    skipSystemComposables = treeSettings.hideSystemNodes,
                    generation = 0,
                    extractAllParameters = true
                )

                if (composeResult != null) {
                    val nodeCreator = ComposeViewNodeCreator(composeResult, treeSettings)
                    return@async nodeCreator.createForViewId(layoutAsyncResult.root?.id ?: 0) { false }
                }
                return@async null
            } catch (e: Exception) {
                logger.e(TAG, "Failed to get Compose data", e)
                return@async null
            }
        }
        val composeNodes = composeDataAsync.await()

        val layoutRootNode = layoutAsyncResult.root
        logger.d("$TAG: prepare capture : dumpViewModeEnabled = ${recordingConfig.recordOptions.dumpViewModeEnabled}, dumpResult != null : ${dumpNodeAsyncResult != null}, layoutRootNode != null: ${layoutRootNode != null}, composeNodes != null: ${composeNodes != null}")

        val result = when {
            composeNodes != null && layoutRootNode != null -> {
                logger.w("$TAG: found compose nodes and layouts, try to merge")
                val treeMerger = TreeMerger(logger)
                LayoutInspectorResult(
                    treeMerger.mergeComposeNodes(layoutRootNode, composeNodes),
                    layoutAsyncResult.previewImage,
                    layoutAsyncResult.data,
                    layoutAsyncResult.options,
                    layoutAsyncResult.error
                )
            }
            recordingConfig.recordOptions.dumpViewModeEnabled && dumpNodeAsyncResult != null && layoutRootNode != null -> {
                logger.w("$TAG: found dump and layouts, try to merge")
                val treeMerger = TreeMerger(logger)
                LayoutInspectorResult(
                    treeMerger.mergeNodes(layoutRootNode, dumpNodeAsyncResult),
                    layoutAsyncResult.previewImage,
                    layoutAsyncResult.data,
                    layoutAsyncResult.options,
                    layoutAsyncResult.error
                )
            }
            else -> layoutAsyncResult
        }

        logger.d("$TAG: capturing is done, error: ${result.error}")
        return result
    }

    private fun getViewDumps(recordingConfig: RecordingConfig): DumpViewNode? {
        logger.d("$TAG: getViewDumps()")
        val dumper = HierarchyDump(recordingConfig.client.device, layoutFileSystem, logger)
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
