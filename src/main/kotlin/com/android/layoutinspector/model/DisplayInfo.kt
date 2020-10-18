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
/**
 * Contains information used to draw selection boxes over each [ViewNode] in layout inspector's
 * preview box. Create using [com.android.layoutinspector.parser.DisplayInfoFactory]
 */
data class DisplayInfo(
    val willNotDraw: Boolean,
    val isVisible: Boolean,
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int,
    val scrollX: Int,
    val scrollY: Int,
    val clipChildren: Boolean,
    val translateX: Float,
    val translateY: Float,
    val scaleX: Float,
    val scaleY: Float,
    val contentDesc: String?
) {
    fun getCopyAtOrigin() : DisplayInfo {
        return this.copy(left = 0, top = 0)
    }
}