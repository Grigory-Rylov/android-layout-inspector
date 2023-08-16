/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.layoutinspector.model

import com.android.ddmlib.Client
import com.android.ddmlib.ClientData
import com.android.ddmlib.DebugViewDumpHandler
import com.android.layoutinspector.LayoutInspectorCaptureOptions
import com.android.layoutinspector.ProtocolVersion
import com.android.layoutinspector.common.AppLogger
import com.google.common.annotations.VisibleForTesting
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

/** Represents a root window.  */
class ClientWindow(
    private val logger: AppLogger,
    val title: String,
    private val client: Client,
    val clientViewInspector: ClientViewInspector = ModernClientViewInspector()
) {
    /**
     * Returns the name for the window suitable for displaying on the UI. Returns the class name if
     * available otherwise returns the component package name.
     */
    val displayName: String?
        get() {
            val appName = client.clientData.clientDescription
            val parts: ArrayList<String> = arrayListOf(*title.split("/").toTypedArray())
            parts.remove("")
            parts.remove(appName)
            return if (parts.isEmpty()) {
                appName
            } else parts[if (parts.size > 2) 1 else 0]
        }

    /** Byte array representing the view hierarchy dump of the window.  */
    suspend fun loadWindowData(
        options: LayoutInspectorCaptureOptions,
        timeout: Long,
        unit: TimeUnit
    ): ByteArray? = clientViewInspector.dumpViewHierarchy(
        logger,
        client,
        title,
        false,
        true,
        options.version == ProtocolVersion.Version2,
        timeout,
        unit
    )

    /** Byte array representing image preview of the provided node.  */
    suspend fun loadViewImage(node: ViewNode, timeout: Long, unit: TimeUnit): ByteArray? =
        clientViewInspector.captureView(logger, client, title, node, timeout, unit)

    private class ListViewRootsHandlerV2(
        private val logger: AppLogger
    ) : DebugViewDumpHandler() {

        private val viewRootsState = MutableStateFlow<List<String>?>(null)

        override fun handleViewDebugResult(data: ByteBuffer) {
            val viewRoots = mutableListOf<String>()
            val nWindows = data.int
            repeat(nWindows) {
                val len = data.int
                viewRoots.add(getString(data, len))
            }
            viewRootsState.value = viewRoots
        }

        suspend fun getWindows(client: Client, timeout: Long, unit: TimeUnit): List<ClientWindow> {
            //TODO: check timeouts
            client.listViewRoots(this)
            val windows = viewRootsState.filterNotNull().first()

            val result = mutableListOf<ClientWindow>()
            for (root in windows) {
                result.add(ClientWindow(logger, root, client))
            }
            return result
        }
    }

    companion object {
        /** Lists all the active window for the current client.  */
        @Throws(IOException::class)
        @JvmStatic
        suspend fun getAllV2(
            logger: AppLogger,
            client: Client, timeout: Long, unit: TimeUnit
        ): List<ClientWindow>? {
            val cd = client.clientData
            return if (cd.hasFeature(ClientData.FEATURE_VIEW_HIERARCHY)) {
                ListViewRootsHandlerV2(logger).getWindows(client, timeout, unit)
            } else null
        }

        /**
         * Convert an integer type to a 4-character string.
         */
        fun chunkName(type: Int): String {
            val ascii = CharArray(4)
            ascii[0] = (type shr 24 and 0xff).toChar()
            ascii[1] = (type shr 16 and 0xff).toChar()
            ascii[2] = (type shr 8 and 0xff).toChar()
            ascii[3] = (type and 0xff).toChar()
            return String(ascii)
        }
    }

    @VisibleForTesting
    interface ClientViewInspector {
        suspend fun dumpViewHierarchy(
            logger: AppLogger,
            client: Client,
            clientWindowTitle: String,
            skipChildren: Boolean,
            includeProperties: Boolean,
            useV2: Boolean,
            timeout: Long,
            timeUnit: TimeUnit
        ): ByteArray?

        suspend fun captureView(
            logger: AppLogger,
            client: Client,
            clientWindowTitle: String,
            node: ViewNode,
            timeout: Long,
            timeUnit: TimeUnit
        ): ByteArray?
    }

    override fun toString(): String {
        return title
    }
}