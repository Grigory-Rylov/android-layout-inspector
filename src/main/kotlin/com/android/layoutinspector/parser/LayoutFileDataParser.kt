/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.android.layoutinspector.parser
import com.android.layoutinspector.LayoutInspectorCaptureOptions
import com.android.layoutinspector.model.LayoutFileData
import com.android.layoutinspector.model.ViewNode
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.ObjectInputStream
import java.nio.file.Files
import javax.imageio.ImageIO
object LayoutFileDataParser {
    /**
     * List of [ViewProperty] to be skipped since the framework won't correctly report their data.
     * See ag/64673340
     */
    @JvmStatic
    val SKIPPED_PROPERTIES = listOf("bg_", "fg_")
    @Throws(IOException::class)
    @JvmStatic
    fun parseFromFile(file: File): LayoutFileData {
        return parseFromBytes(Files.readAllBytes(file.toPath()))
    }
    @Throws(IOException::class)
    @JvmStatic
    fun parseFromBytes(
        bytes: ByteArray,
        skippedProperties: Collection<String> = SKIPPED_PROPERTIES
    ): LayoutFileData {
        val bufferedImage: BufferedImage?
        var node: ViewNode? = null
        var options = LayoutInspectorCaptureOptions()
        var previewBytes = ByteArray(0)
        ObjectInputStream(ByteArrayInputStream(bytes)).use { input ->
            // Parse options
            options.parse(input.readUTF())
            // Parse view node
            val nodeBytes = ByteArray(input.readInt())
            input.readFully(nodeBytes)
            node = ViewNodeParser.parse(nodeBytes, options.version, skippedProperties)
            if (node == null) {
                throw IOException("Error parsing view node")
            }
            // Preview image
            previewBytes = ByteArray(input.readInt())
            input.readFully(previewBytes)
        }
        bufferedImage = ImageIO.read(ByteArrayInputStream(previewBytes))
        return LayoutFileData(bufferedImage, node, options)
    }
}