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

import com.google.common.collect.Lists
import com.google.common.collect.Maps
import java.awt.Rectangle
import java.util.*
import javax.swing.tree.TreeNode
import javax.swing.tree.TreePath

private const val MAX_TEXT_LENGTH = 20

/**
 * Represents an Android View object. Holds properties and a previewBox that contains the display area
 * of the object on screen.
 * Created by parsing view dumps using [com.android.layoutinspector.parser.ViewNodeParser].
 */
// make parent private because it's the same as the getParent method from TreeNode
data class ViewNode constructor(private val parent: ViewNode?, val name: String, val hash: String) :
    TreeNode {
    // If the force state is set, the preview tries to render/hide the view
    // (depending on the parent's state)
    enum class ForcedState {
        NONE,
        VISIBLE,
        INVISIBLE
    }

    val groupedProperties: MutableMap<String, MutableList<ViewProperty>> = Maps.newHashMap()
    val namedProperties: MutableMap<String, ViewProperty> = Maps.newHashMap()
    val properties: MutableList<ViewProperty> = Lists.newArrayList()
    val children: MutableList<ViewNode> = Lists.newArrayList()
    val previewBox: Rectangle = Rectangle()
    // default in case properties are not available
    var index: Int = 0
    var id: String? = null
    // TODO(kelvinhanma) get rid of lateinit by refactoring creation of DisplayInfo
    var displayInfo: DisplayInfo = DisplayInfo.createEmpty()
        set(value) {
            field = value
            initLocationOnScreen()
        }

    var isParentVisible: Boolean = false
        private set
    var isDrawn: Boolean = false
        private set
    var forcedState: ForcedState = ForcedState.NONE
    var locationOnScreenX: Int = 0
        private set
    var locationOnScreenY: Int = 0
        private set

    private fun initLocationOnScreen() {
        val xProperty = getProperty("layout:getLocationOnScreen_x()")
        val yProperty = getProperty("layout:getLocationOnScreen_y()")
        if (xProperty != null && yProperty != null) {
            locationOnScreenX = xProperty.intValue
            locationOnScreenY = yProperty.intValue
        } else {
            val parentX = parent?.locationOnScreenX ?: 0
            val parentY = parent?.locationOnScreenY ?: 0

            locationOnScreenX = displayInfo.left + displayInfo.translateX.toInt() + parentX
            locationOnScreenY = displayInfo.top + displayInfo.translateY.toInt() + parentY
        }
    }

    fun addPropertyToGroup(property: ViewProperty) {
        val key = getKey(property)
        val propertiesList = groupedProperties.getOrDefault(
            key,
            LinkedList()
        )
        propertiesList.add(property)
        groupedProperties[key] = propertiesList
    }

    private fun getKey(property: ViewProperty): String {
        return property.category ?: if (property.fullName.endsWith("()")) {
            "methods"
        } else {
            "properties"
        }
    }

    fun getProperty(name: String, vararg altNames: String): ViewProperty? {
        var property: ViewProperty? = namedProperties[name]
        var i = 0
        while (property == null && i < altNames.size) {
            property = namedProperties[altNames[i]]
            i++
        }
        return property
    }

    /** Recursively updates all the visibility parameter of the nodes.  */
    fun updateNodeDrawn() {
        updateNodeDrawn(isParentVisible)
    }

    fun updateNodeDrawn(parentVisible: Boolean) {
        var parentVisible = parentVisible
        isParentVisible = parentVisible
        if (forcedState == ForcedState.NONE) {
            isDrawn = !displayInfo.willNotDraw && parentVisible && displayInfo.isVisible
            parentVisible = parentVisible and displayInfo.isVisible
        } else {
            isDrawn = forcedState == ForcedState.VISIBLE && parentVisible
            parentVisible = isDrawn
        }
        for (child in children) {
            child.updateNodeDrawn(parentVisible)
            isDrawn = isDrawn or (child.isDrawn && child.displayInfo.isVisible)
        }
    }

    fun getFormattedName(): String {
        val idPrefix = if (id != null && id != "NO_ID") id else null

        val text = getText()
        val typeAsString = typeAsString()

        if (text != null) {
            if (idPrefix != null) {
                return "$idPrefix ($typeAsString) - \"$text\""
            }
            return "$typeAsString - \"$text\""
        }
        if (idPrefix != null) {
            return "$idPrefix ($typeAsString)"
        }
        return typeAsString
    }

    override fun toString() = "$name@$hash"

    override fun getChildAt(childIndex: Int): ViewNode {
        return children[childIndex]
    }

    override fun getChildCount(): Int {
        return children.size
    }

    override fun getParent(): ViewNode? {
        return parent
    }

    override fun getIndex(node: TreeNode): Int {
        return children.indexOf(node as ViewNode)
    }

    override fun getAllowsChildren(): Boolean {
        return true
    }

    override fun isLeaf(): Boolean {
        return childCount == 0
    }

    override fun children(): Enumeration<out TreeNode> {
        return Collections.enumeration(children)
    }

    fun typeAsString(): String {
        val lastDotPost = name.lastIndexOf(".")
        if (lastDotPost >= 0) {
            return name.substring(lastDotPost + 1)
        }
        return name
    }

    fun getElliptizedText(text: String): String {
        if (text.length <= MAX_TEXT_LENGTH) {
            return text
        }
        return text.substring(0, MAX_TEXT_LENGTH) + "…"
    }

    fun getText(): String? {
        return namedProperties["text:mText"]?.value
    }

    companion object {
        /** Finds the path from node to the root.  */
        @JvmStatic
        fun getPath(node: ViewNode): TreePath {
            return getPathImpl(node, null)
        }

        /** Finds the path from node to the parent.  */
        @JvmStatic
        fun getPathFromParent(node: ViewNode, root: ViewNode): TreePath {
            return getPathImpl(node, root)
        }

        private fun getPathImpl(node: ViewNode, root: ViewNode?): TreePath {
            var node: ViewNode? = node
            val nodes = Lists.newArrayList<Any>()
            do {
                nodes.add(0, node)
                node = node?.parent
            } while (node != null && node !== root)
            if (root != null && node === root) {
                nodes.add(0, root)
            }
            return TreePath(nodes.toTypedArray())
        }
    }
}
