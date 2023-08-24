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
package com.android.layoutinspector.model

import com.android.layoutinspector.LayoutInspectorCaptureOptions
import com.android.layoutinspector.LayoutInspectorResult
import com.github.grishberg.android.layoutinspector.domain.AbstractViewNode
import java.awt.image.BufferedImage

/**
 * Data model for a parsed .li file. Create using methods in [com.android.layoutinspector.parser.LayoutFileDataParser]
 */
data class LayoutFileData(
    val bufferedImage: BufferedImage?,
    val node: AbstractViewNode?,
    val options: LayoutInspectorCaptureOptions
) {
    companion object {
        fun fromLayoutInspectorResult(result: LayoutInspectorResult): LayoutFileData {
            return LayoutFileData(result.previewImage, result.root, result.options!!)
        }
    }
}
