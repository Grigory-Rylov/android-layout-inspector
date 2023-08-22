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
package com.android.layoutinspector

import com.android.layoutinspector.model.ViewNode
import com.github.grishberg.android.layoutinspector.domain.AbstractViewNode
import com.github.grishberg.android.layoutinspector.domain.DumpViewNode
import java.awt.image.BufferedImage


/**
 * Represents result of a capture
 * Success: data is not null, and error is the empty string
 * Error: data is null, and error a non empty error message
 */
class LayoutInspectorResult(
    val root: AbstractViewNode?,
    var dumpViewRoot: AbstractViewNode?,
    val previewImage: BufferedImage?,
    val data: ByteArray?,
    val options: LayoutInspectorCaptureOptions?,
    val error: String,
) {
    companion object {
        fun createErrorResult(error: String) = LayoutInspectorResult(
            root = null,
            dumpViewRoot = null,
            previewImage = null,
            data = null,
            options = null,
            error = error
        )
    }
}
