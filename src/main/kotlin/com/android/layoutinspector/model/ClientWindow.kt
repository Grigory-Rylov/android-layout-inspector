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
import com.android.ddmlib.ChunkHandler
import com.android.ddmlib.Client
import com.android.ddmlib.ClientData
import com.android.ddmlib.HandleViewDebug
import com.android.layoutinspector.LayoutInspectorCaptureOptions
import com.android.layoutinspector.ProtocolVersion
import com.google.common.annotations.VisibleForTesting
import com.google.common.collect.Lists
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
/** Represents a root window.  */
class ClientWindow(val title: String, private val client: Client, val clientViewInspector: ClientViewInspector = object : ClientViewInspector {}) {
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
    fun loadWindowData(
        options: LayoutInspectorCaptureOptions,
        timeout: Long,
        unit: TimeUnit
    ): ByteArray? = clientViewInspector.dumpViewHierarchy(
        client,
        title,
        false,
        true,
        options.version == ProtocolVersion.Version2,
        timeout,
        unit
    )
    /** Byte array representing image preview of the provided node.  */
    fun loadViewImage(node: ViewNode, timeout: Long, unit: TimeUnit): ByteArray? =
        clientViewInspector.captureView(client, title, node, timeout, unit)
    private class ListViewRootsHandler :
        HandleViewDebug.ViewDumpHandler(HandleViewDebug.CHUNK_VULW) {
        private val myViewRoots = Lists.newCopyOnWriteArrayList<String>()
        override fun handleViewDebugResult(data: ByteBuffer) {
            val nWindows = data.int
            for (i in 0 until nWindows) {
                val len = data.int
                myViewRoots.add(ChunkHandler.getString(data, len))
            }
        }
        @Throws(IOException::class)
        fun getWindows(c: Client, timeout: Long, unit: TimeUnit): List<ClientWindow> {
            HandleViewDebug.listViewRoots(c, this)
            waitForResult(timeout, unit)
            val windows = Lists.newArrayList<ClientWindow>()
            for (root in myViewRoots) {
                windows.add(ClientWindow(root, c))
            }
            return windows
        }
    }
    private class CaptureByteArrayHandler(type: Int) : HandleViewDebug.ViewDumpHandler(type) {
        private val mData = AtomicReference<ByteArray>()
        override fun handleViewDebugResult(data: ByteBuffer) {
            val b = ByteArray(data.remaining())
            data.get(b)
            mData.set(b)
        }
        fun getData(timeout: Long, unit: TimeUnit): ByteArray? {
            waitForResult(timeout, unit)
            return mData.get()
        }
    }
    companion object {
        /** Lists all the active window for the current client.  */
        @Throws(IOException::class)
        @JvmStatic
        fun getAll(
            client: Client, timeout: Long, unit: TimeUnit
        ): List<ClientWindow>? {
            val cd = client.clientData
            return if (cd.hasFeature(ClientData.FEATURE_VIEW_HIERARCHY)) {
                ListViewRootsHandler().getWindows(client, timeout, unit)
            } else null
        }
    }
    @VisibleForTesting
    interface ClientViewInspector {
        fun dumpViewHierarchy(
            client: Client,
            title: String,
            skipChildren: Boolean,
            includeProperties: Boolean,
            useV2: Boolean,
            timeout: Long,
            timeUnit: TimeUnit): ByteArray? {
            val handler = CaptureByteArrayHandler(HandleViewDebug.CHUNK_VURT)
            HandleViewDebug.dumpViewHierarchy(
                client, title, skipChildren, includeProperties, useV2, handler
            )
            return try {
                handler.getData(timeout, timeUnit)
            } catch(e: IOException) {
                null
            }
        }
        fun captureView(
            client: Client,
            title: String,
            node: ViewNode,
            timeout: Long,
            timeUnit: TimeUnit): ByteArray? {
            val handler = CaptureByteArrayHandler(HandleViewDebug.CHUNK_VUOP)
            HandleViewDebug.captureView(client, title, node.toString(), handler)
            return try {
                handler.getData(timeout, timeUnit)
            } catch(e: IOException) {
                null
            }
        }
    }

    override fun toString(): String {
        return title
    }
}