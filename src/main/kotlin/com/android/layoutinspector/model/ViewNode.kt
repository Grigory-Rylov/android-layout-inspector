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

import com.github.grishberg.android.layoutinspector.domain.AbstractViewNode
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import java.awt.Rectangle
import java.util.Collections
import java.util.Enumeration
import java.util.LinkedList
import javax.swing.tree.TreeNode
import javax.swing.tree.TreePath

/**
 * Represents an Android View object. Holds properties and a previewBox that contains the display area
 * of the object on screen.
 * Created by parsing view dumps using [com.android.layoutinspector.parser.ViewNodeParser].
 */
// make parent private because it's the same as the getParent method from TreeNode
data class ViewNode constructor(
    private val parent: ViewNode?,
    override val name: String,
    override val hash: String,
    val namedProperties: Map<String, ViewProperty> = Maps.newHashMap(),
    val properties: List<ViewProperty> = Lists.newArrayList(),
    override val children: MutableList<AbstractViewNode> = Lists.newArrayList(),
    var displayInfo: DisplayInfo = DisplayInfo(false, false, 0, 0, 0, 0, 0, 0, false, 0f, 0f, 0f, 0f, null),
) : AbstractViewNode {
    // If the force state is set, the preview tries to render/hide the view
    // (depending on the parent's state)
    enum class ForcedState {
        NONE,
        VISIBLE,
        INVISIBLE
    }

    val previewBox: Rectangle = Rectangle()
    // TODO: make immutable.
    val groupedProperties: MutableMap<String, MutableList<ViewProperty>>

    // default in case properties are not available
    var index: Int = 0
    override var id: String? = null

    var isParentVisible: Boolean = false
        private set
    var isDrawn: Boolean = false
        private set
    var forcedState: ForcedState = ForcedState.NONE


    override val locationOnScreenX: Int
    override val locationOnScreenY: Int
    override val width: Int
        get() = displayInfo.width
    override val height: Int
        get() = displayInfo.height

    override val isVisible: Boolean
        get() = displayInfo.isVisible

    override val typeAsString: String
        get() = calculateTypeAsString()

    override val text: String?
        get() = namedProperties["text:mText"]?.value

    init {
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

        groupedProperties = createGroupProperties()
    }

    private fun createGroupProperties(): MutableMap<String, MutableList<ViewProperty>> {
        val groupedPropertiesBuffer: MutableMap<String, MutableList<ViewProperty>> = Maps.newHashMap()
        properties.map { property ->
            val key = getKey(property)
            val propertiesList = groupedPropertiesBuffer.getOrDefault(
                key,
                LinkedList()
            )
            propertiesList.add(property)
            groupedPropertiesBuffer[key] = propertiesList
        }
        return groupedPropertiesBuffer
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
            if (child is ViewNode) {
                child.updateNodeDrawn(parentVisible)
                isDrawn = isDrawn or (child.isDrawn && child.displayInfo.isVisible)
            }
        }
    }
    override fun toString() = "$name@$hash"

    override fun getChildAt(childIndex: Int): AbstractViewNode {
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

    override fun isLeaf(): Boolean = childCount == 0

    override fun children(): Enumeration<out TreeNode> {
        return Collections.enumeration(children)
    }

    private fun calculateTypeAsString(): String {
        val lastDotPost = name.lastIndexOf(".")
        if (lastDotPost >= 0) {
            return name.substring(lastDotPost + 1)
        }
        return name
    }

    fun replaceChildren(newChildren: List<AbstractViewNode>) {
        children.clear()
        children.addAll(newChildren.map { child -> child.cloneWithNewParent(this) })
    }

    override fun cloneWithNewParent(newParent: AbstractViewNode): AbstractViewNode {
        throw IllegalStateException("I was not ready for this operation =(")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ViewNode

        if (name != other.name) return false
        if (hash != other.hash) return false
        if (namedProperties != other.namedProperties) return false
        if (properties != other.properties) return false
        if (index != other.index) return false
        if (id != other.id) return false
        if (isParentVisible != other.isParentVisible) return false
        if (isDrawn != other.isDrawn) return false
        if (forcedState != other.forcedState) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + hash.hashCode()
        result = 31 * result + namedProperties.hashCode()
        result = 31 * result + properties.hashCode()
        result = 31 * result + index
        result = 31 * result + (id?.hashCode() ?: 0)
        result = 31 * result + isParentVisible.hashCode()
        result = 31 * result + isDrawn.hashCode()
        result = 31 * result + forcedState.hashCode()
        return result
    }

}
