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
import com.android.layoutinspector.model.ViewNode
import com.android.layoutinspector.model.ViewProperty
object DisplayInfoFactory {
    fun createDisplayInfoFromNode(node: ViewNode): DisplayInfo {
        val left = getInt(node.getProperty("mLeft", "layout:mLeft", "left"), 0)
        val top = getInt(node.getProperty("mTop", "layout:mTop", "top"), 0)
        val width = getInt(node.getProperty("getWidth()", "layout:getWidth()", "width"), 10)
        val height = getInt(node.getProperty("getHeight()", "layout:getHeight()", "height"), 10)
        val scrollX = getInt(node.getProperty("mScrollX", "scrolling:mScrollX", "scrollX"), 0)
        val scrollY = getInt(node.getProperty("mScrollY", "scrolling:mScrollY", "scrollY"), 0)
        val willNotDraw =
            getBoolean(node.getProperty("willNotDraw()", "drawing:willNotDraw()", "willNotDraw"), false)
        val clipChildren = getBoolean(
            node.getProperty("getClipChildren()", "drawing:getClipChildren()", "clipChildren"), true
        )
        val translateX =
            getFloat(node.getProperty("getTranslationX", "drawing:getTranslationX()", "translationX"), 0f)
        val translateY =
            getFloat(node.getProperty("getTranslationY", "drawing:getTranslationY()", "translationY"), 0f)
        val scaleX = getFloat(node.getProperty("getScaleX()", "drawing:getScaleX()", "scaleX"), 1f)
        val scaleY = getFloat(node.getProperty("getScaleY()", "drawing:getScaleY()", "scaleY"), 1f)
        var descProp = node.getProperty("accessibility:getContentDescription()", "contentDescription")
        var contentDescription: String? = if (descProp != null && descProp.value != "null")
            descProp.value
        else
            null
        if (contentDescription == null) {
            descProp = node.getProperty("text:mText")
            contentDescription = if (descProp != null && descProp.value != "null")
                descProp.value
            else
                null
        }
        val visibility = node.getProperty("getVisibility()", "misc:getVisibility()", "visibility")
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