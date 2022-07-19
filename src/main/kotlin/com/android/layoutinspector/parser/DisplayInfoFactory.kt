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
import com.android.layoutinspector.model.DisplayInfo
import com.android.layoutinspector.model.ViewProperty
object DisplayInfoFactory {
    fun createDisplayInfoFromNamedProperties(namedProperties: Map<String, ViewProperty>): DisplayInfo {
        val left = getInt(getProperty(namedProperties, "mLeft", "layout:mLeft", "left"), 0)
        val top = getInt(getProperty(namedProperties, "mTop", "layout:mTop", "top"), 0)
        val width = getInt(getProperty(namedProperties, "getWidth()", "layout:getWidth()", "width"), 10)
        val height = getInt(getProperty(namedProperties, "getHeight()", "layout:getHeight()", "height"), 10)
        val scrollX = getInt(getProperty(namedProperties, "mScrollX", "scrolling:mScrollX", "scrollX"), 0)
        val scrollY = getInt(getProperty(namedProperties, "mScrollY", "scrolling:mScrollY", "scrollY"), 0)
        val willNotDraw =
            getBoolean(getProperty(namedProperties, "willNotDraw()", "drawing:willNotDraw()", "willNotDraw"), false)
        val clipChildren = getBoolean(
            getProperty(namedProperties, "getClipChildren()", "drawing:getClipChildren()", "clipChildren"), true
        )
        val translateX =
            getFloat(getProperty(namedProperties, "getTranslationX", "drawing:getTranslationX()", "translationX"), 0f)
        val translateY =
            getFloat(getProperty(namedProperties, "getTranslationY", "drawing:getTranslationY()", "translationY"), 0f)
        val scaleX = getFloat(getProperty(namedProperties, "getScaleX()", "drawing:getScaleX()", "scaleX"), 1f)
        val scaleY = getFloat(getProperty(namedProperties, "getScaleY()", "drawing:getScaleY()", "scaleY"), 1f)
        var descProp = getProperty(namedProperties, "accessibility:getContentDescription()", "contentDescription")
        var contentDescription: String? = if (descProp != null && descProp.value != "null")
            descProp.value
        else
            null
        if (contentDescription == null) {
            descProp = getProperty(namedProperties, "text:mText")
            contentDescription = if (descProp != null && descProp.value != "null")
                descProp.value
            else
                null
        }
        val visibility = getProperty(namedProperties, "getVisibility()", "misc:getVisibility()", "visibility")
        val isVisible = (visibility == null
                || "0" == visibility.value
                || "VISIBLE" == visibility.value)
        return DisplayInfo(
            willNotDraw,
            isVisible,
            left,
            top,
            width,
            height,
            scrollX,
            scrollY,
            clipChildren,
            translateX,
            translateY,
            scaleX,
            scaleY,
            contentDescription
        )
    }
    private fun getProperty(namedProperties: Map<String, ViewProperty>, name: String, vararg altNames: String): ViewProperty? {
        var property: ViewProperty? = namedProperties[name]
        var i = 0
        while (property == null && i < altNames.size) {
            property = namedProperties[altNames[i]]
            i++
        }
        return property
    }

    private fun getBoolean(p: ViewProperty?, defaultValue: Boolean): Boolean {
        if (p != null) {
            return try {
                java.lang.Boolean.parseBoolean(p.value)
            } catch (e: NumberFormatException) {
                defaultValue
            }
        }
        return defaultValue
    }
    private fun getInt(p: ViewProperty?, defaultValue: Int): Int {
        if (p != null) {
            return try {
                Integer.parseInt(p.value)
            } catch (e: NumberFormatException) {
                defaultValue
            }
        }
        return defaultValue
    }
    private fun getFloat(p: ViewProperty?, defaultValue: Float): Float {
        if (p != null) {
            return try {
                java.lang.Float.parseFloat(p.value)
            } catch (e: NumberFormatException) {
                defaultValue
            }
        }
        return defaultValue
    }
}