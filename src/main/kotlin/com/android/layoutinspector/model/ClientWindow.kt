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

import com.android.ddmlib.ByteBufferUtil
import com.android.ddmlib.Client
import com.android.ddmlib.ClientData
import com.android.ddmlib.DebugViewDumpHandler
import com.android.ddmlib.internal.ClientImpl
import com.android.ddmlib.internal.jdwp.chunkhandler.ChunkHandler
import com.android.ddmlib.internal.jdwp.chunkhandler.HandleViewDebug
import com.android.layoutinspector.LayoutInspectorCaptureOptions
import com.android.layoutinspector.ProtocolVersion
import com.android.layoutinspector.common.AppLogger
import com.google.common.annotations.VisibleForTesting
import com.google.common.collect.Lists
import java.io.IOException
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Represents a root window.  */
class ClientWindow(
    private val logger: AppLogger,
    val title: String,
    private val client: Client,
    val clientViewInspector: ClientViewInspector = object : ClientViewInspector {}
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
    fun loadWindowData(
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
    fun loadViewImage(node: ViewNode, timeout: Long, unit: TimeUnit): ByteArray? =
        clientViewInspector.captureView(logger, client, title, node, timeout, unit)

    private class ListViewRootsHandler(
        private val logger: AppLogger
    ) : DebugViewDumpHandler(DebugViewDumpHandler.CHUNK_VULW) {
        private val myViewRoots = Lists.newCopyOnWriteArrayList<String>()
        override fun handleViewDebugResult(data: ByteBuffer) {
            val nWindows = data.int
            for (i in 0 until nWindows) {
                val len = data.int
                myViewRoots.add(ByteBufferUtil.getString(data, len))
            }
        }

        @Throws(IOException::class)
        fun getWindows(c: Client, timeout: Long, unit: TimeUnit): List<ClientWindow> {
            HandleViewDebug.listViewRoots(c, this)
            waitForResult(timeout, unit)
            val windows = Lists.newArrayList<ClientWindow>()
            for (root in myViewRoots) {
                windows.add(ClientWindow(logger, root, c))
            }
            return windows
        }
    }

    private class CaptureByteArrayHandler(
        private val logger: AppLogger, type: Int
    ) : DebugViewDumpHandler(type) {
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

        override fun handleUnknownChunk(
            client: ClientImpl?,
            type: Int,
            data: ByteBuffer?,
            isReply: Boolean,
            msgId: Int
        ) {
            if (type == ChunkHandler.CHUNK_FAIL) {
                val errorCode: Int
                val msgLen: Int
                val msg: String
                errorCode = data!!.int
                msgLen = data.int
                msg = ByteBufferUtil.getString(data, msgLen)
                logger.w("ddms: WARNING: failure code=$errorCode msg=$msg")
            } else {
                logger.w(
                    "ddms: WARNING: received unknown chunk " + chunkName(type)
                            + ": len=" + data!!.limit() + ", reply=" + isReply
                            + ", msgId=0x" + Integer.toHexString(msgId)
                )
            }
            logger.w("ddms:        client $client, handler $this")
        }
    }

    companion object {
        /** Lists all the active window for the current client.  */
        @Throws(IOException::class)
        @JvmStatic
        fun getAll(
            logger: AppLogger,
            client: Client, timeout: Long, unit: TimeUnit
        ): List<ClientWindow>? {
            val cd = client.clientData
            return if (cd.hasFeature(ClientData.FEATURE_VIEW_HIERARCHY)) {
                ListViewRootsHandler(logger).getWindows(client, timeout, unit)
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
        fun dumpViewHierarchy(
            logger: AppLogger,
            client: Client,
            title: String,
            skipChildren: Boolean,
            includeProperties: Boolean,
            useV2: Boolean,
            timeout: Long,
            timeUnit: TimeUnit
        ): ByteArray? {
            val handler = CaptureByteArrayHandler(logger, DebugViewDumpHandler.CHUNK_VURT)
            HandleViewDebug.dumpViewHierarchy(
                client, title, skipChildren, includeProperties, useV2, handler
            )
            return try {
                handler.getData(timeout, timeUnit)
            } catch (e: IOException) {
                null
            }
        }

        fun captureView(
            logger: AppLogger,
            client: Client,
            title: String,
            node: ViewNode,
            timeout: Long,
            timeUnit: TimeUnit
        ): ByteArray? {
            val handler = CaptureByteArrayHandler(logger, DebugViewDumpHandler.CHUNK_VUOP)
            HandleViewDebug.captureView(client, title, node.toString(), handler)
            return try {
                handler.getData(timeout, timeUnit)
            } catch (e: IOException) {
                null
            }
        }
    }

    override fun toString(): String {
        return title
    }
}