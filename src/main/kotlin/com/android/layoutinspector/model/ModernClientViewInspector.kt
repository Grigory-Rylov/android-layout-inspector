package com.android.layoutinspector.model

import com.android.ddmlib.Client
import com.android.ddmlib.DebugViewDumpHandler
import com.android.layoutinspector.common.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

class ModernClientViewInspector : ClientWindow.ClientViewInspector {
    override suspend fun dumpViewHierarchy(
        logger: AppLogger,
        client: Client,
        clientWindowTitle: String,
        skipChildren: Boolean,
        includeProperties: Boolean,
        useV2: Boolean,
        timeout: Long,
        timeUnit: TimeUnit
    ): ByteArray? {
        val handler = DumpViewHierarchyDebugViewDumpHandler()
        client.dumpViewHierarchy(clientWindowTitle, skipChildren, includeProperties, useV2, handler)
        return handler.value.filterNotNull().first()
    }

    private inner class DumpViewHierarchyDebugViewDumpHandler : DebugViewDumpHandler() {
        val value = MutableStateFlow<ByteArray?>(null)
        override fun handleViewDebugResult(data: ByteBuffer?) {
            value.value = data?.array()
        }
    }

    override suspend fun captureView(
        logger: AppLogger,
        client: Client,
        clientWindowTitle: String,
        node: ViewNode,
        timeout: Long,
        timeUnit: TimeUnit
    ): ByteArray? {
        val handler = CaptureViewDebugViewDumpHandler()
        client.captureView(clientWindowTitle, node.toString(), handler)
        return handler.value.filterNotNull().first()
    }

    private inner class CaptureViewDebugViewDumpHandler : DebugViewDumpHandler() {
        val value = MutableStateFlow<ByteArray?>(null)

        override fun handleViewDebugResult(data: ByteBuffer?) {
            value.value = data?.array()
        }
    }
}