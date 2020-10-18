package com.github.grishberg.android.layoutinspector.process

import com.android.layoutinspector.LayoutInspectorBridge
import com.android.layoutinspector.LayoutInspectorCaptureOptions
import com.android.layoutinspector.LayoutInspectorResult
import com.android.layoutinspector.common.AppLogger
import com.android.layoutinspector.model.ClientWindow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

private const val TAG = "LayoutInspectorCaptureTask"

class LayoutInspectorCaptureTask(
    private val logger: AppLogger
) {
    suspend fun capture(clientWindow: ClientWindow, timeoutInSeconds: Int): LayoutInspectorResult {
        val result = GlobalScope.async(Dispatchers.IO) {
            logger.d("$TAG: start capture view timeout = $timeoutInSeconds")

            try {
                val captureView = LayoutInspectorBridge.captureView(
                    logger,
                    clientWindow,
                    LayoutInspectorCaptureOptions(),
                    timeoutInSeconds.toLong()
                )
                return@async captureView
            } catch (e: Exception) {
                logger.e("$TAG", e)
                throw e
            }
        }
        val await = result.await()
        logger.d("$TAG: capturing is done, error: ${await.error}")
        return await
    }
}
