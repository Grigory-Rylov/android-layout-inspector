package com.github.grishberg.android.layoutinspector.process

import com.android.layoutinspector.LayoutInspectorBridge
import com.android.layoutinspector.LayoutInspectorCaptureOptions
import com.android.layoutinspector.LayoutInspectorResult
import com.android.layoutinspector.model.ClientWindow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

class LayoutInspectorCaptureTask() {
    suspend fun capture(clientWindow: ClientWindow, timeoutInSeconds: Int): LayoutInspectorResult {
        val result = GlobalScope.async(Dispatchers.IO) {
            return@async LayoutInspectorBridge.captureView(
                clientWindow,
                LayoutInspectorCaptureOptions(),
                timeoutInSeconds.toLong()
            )
        }
        return result.await()
    }
}
