package com.github.grishberg.android.layoutinspector.process

import com.android.layoutinspector.LayoutInspectorBridge
import com.android.layoutinspector.LayoutInspectorBridge.V2_MIN_API
import com.android.layoutinspector.LayoutInspectorCaptureOptions
import com.android.layoutinspector.LayoutInspectorResult
import com.android.layoutinspector.ProtocolVersion
import com.android.layoutinspector.common.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async


private const val TAG = "LayoutInspectorCaptureTask"

class LayoutInspectorCaptureTask(
    private val scope: CoroutineScope,
    private val logger: AppLogger,
) {
    suspend fun capture(recordingConfig: RecordingConfig): LayoutInspectorResult {
        val result = scope.async(Dispatchers.IO) {
            logger.d("$TAG: start capture view timeout = ${recordingConfig.timeoutInSeconds}")

            try {
                val version: ProtocolVersion = determineProtocolVersion(
                    recordingConfig.client.device.version.apiLevel, recordingConfig.v2Enabled
                )
                val options = LayoutInspectorCaptureOptions()
                options.version = version

                val captureView = LayoutInspectorBridge.captureView(
                    logger,
                    recordingConfig.clientWindow,
                    options,
                    recordingConfig.timeoutInSeconds.toLong()
                )
                return@async captureView
            } catch (e: Exception) {
                logger.e(TAG, e)
                throw e
            }
        }
        val await = result.await()
        logger.d("$TAG: capturing is done, error: ${await.error}")
        return await
    }

    private fun determineProtocolVersion(apiVersion: Int, v2Enabled: Boolean): ProtocolVersion {
        return if (apiVersion >= V2_MIN_API && v2Enabled) ProtocolVersion.Version2 else ProtocolVersion.Version1
    }
}
