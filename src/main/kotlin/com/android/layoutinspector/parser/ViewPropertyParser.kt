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

import com.android.layoutinspector.model.ViewProperty

object ViewPropertyParser {
    private val availableProprtyFullNames = listOf(
        "layout:mBottom", "layout:mLeft", "layout:mRight", "layout:mTop",
        "layout:getHeight()", "layout:getWidth()", "layout:getBaseline()", "layout:layout_bottomMargin",
        "layout:layout_endMargin", "layout:layout_leftMargin", "layout:layout_rightMargin", "layout:layout_startMargin",
        "layout:layout_topMargin", "layout:layout_height", "layout:layout_width", "layout:getWidth()",
        "measurement:mMeasuredHeight", "measurement:mMeasuredWidth", "measurement:mMinHeight", "measurement:mMinWidth",
        "measurement:getMeasuredHeightAndState()", "measurement:getMeasuredWidthAndState()",
        "drawing:getPivotX()", "drawing:getPivotY()", "drawing:getTranslationX()", "drawing:getTranslationY()",
        "drawing:getTranslationZ()", "drawing:getX()", "drawing:getY()", "drawing:getZ()",
// protocol v2
        "layout:left", "layout:right",
        "layout:bottom", "layout:top",
        "layout:width", "layout:height",
        "measurement:measuredWidth", "measurement:minWidth", "measurement:measuredHeight", "measurement:minHeight",
        "drawing:translationX", "drawing:translationY", "drawing:translationZ",
        "drawing:pivotX", "drawing:pivotY"
    )

    fun parse(propertyFullName: String, value: String): ViewProperty {
        val colonIndex = propertyFullName.indexOf(':')
        var category: String?
        var name: String?
        if (colonIndex != -1) {
            category = propertyFullName.substring(0, colonIndex)
            name = propertyFullName.substring(colonIndex + 1)
        } else {
            category = null
            name = propertyFullName
        }
        var isSizeProperty = isSizeProperty(category, propertyFullName, value)
        var intValue = 0

        try {
            intValue = Integer.valueOf(value)
        } catch (e: NumberFormatException) {
            isSizeProperty = false
        }
        return ViewProperty(propertyFullName, name, category, value, isSizeProperty, intValue)
    }

    private fun isSizeProperty(category: String?, propertyFullName: String, value: String): Boolean {
        if (value == "null" || value == "-1" || value == "0" || value == "-2147483648") {
            return false
        }
        if (category == "padding") {
            return true
        }
        return availableProprtyFullNames.contains(propertyFullName)
    }
}