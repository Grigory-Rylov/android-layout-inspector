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
import com.android.layoutinspector.model.ViewNode
import com.android.layoutinspector.model.ViewProperty
import com.google.common.base.Verify.verify
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import java.nio.ByteBuffer
import java.util.HashMap
private const val META_KEY = "meta"
private const val HASH_KEY = "$META_KEY:__hash__"
private const val NAME_KEY = "$META_KEY:__name__"
private const val CHILD_COUNT_KEY = "__childCount__"
private const val CHILD_KEY = "__child__"
class ViewNodeV2Parser {
    private var ids: Map<String, Short>? = null
    private var mStringTable: Map<Short, Any>? = null
    private val mViews: MutableList<Map<Short, Any>> = Lists.newArrayListWithExpectedSize(100)
    fun parse(data: ByteArray): ViewNode? {
        val d = ViewNodeV2Decoder(ByteBuffer.wrap(data))
        while (d.hasRemaining()) {
            val o = d.readObject()
            if (o is Map<*, *>) {
                mViews.add(o as Map<Short, Any>)
            }
        }
        if (mViews.isEmpty()) {
            return null
        }
        // the last one is the property map
        mStringTable = mViews.removeAt(mViews.size - 1)
        ids = reverse(mStringTable!!)
        val rootMap = mViews[0]
        val root = createViewNode(rootMap)
        root.updateNodeDrawn(true)
        return root
    }
    private fun createViewNode(
        propMap: Map<Short, Any>,
        parent: ViewNode? = null
    ): ViewNode {
        // create ViewNode
        val hashProperty = getProperty(propMap, HASH_KEY)
        var hash = ""
        if (hashProperty is Int) {
            hash = Integer.toHexString(hashProperty)
        }
        val node = ViewNode(parent, getStringProperty(propMap, NAME_KEY), hash)
        loadProperties(node, propMap)
        node.displayInfo = DisplayInfoFactory.createDisplayInfoFromNode(node)
        return node
    }
    private fun getProperty(props: Map<Short, Any>, key: String): Any? {
        return props[ids!![key]]
    }
    private fun reverse(m: Map<Short, Any>): Map<String, Short> {
        val r = HashMap<String, Short>(m.size)
        for ((key, value) in m) {
            r[value as String] = key
        }
        return r
    }
    private fun getPropertyKey(name: String): Short? {
        return ids!![name]
    }
    private fun getPropertyName(key: Short): String? {
        val v = mStringTable!![key]
        return v as? String
    }
    private fun getStringProperty(view: Map<Short, Any>, key: String): String {
        val v = view[getPropertyKey(key)]
        if (v is String) {
            return v
        }
        return ""
    }
    private fun getChildIndex(name: String): Int {
        return name.substring(name.indexOf(CHILD_KEY) + 9).toInt()
    }
    private fun loadProperties(node: ViewNode, viewProperties: Map<Short, Any>) {
        val namedProperties: MutableMap<String, ViewProperty> = Maps.newHashMap()
        val properties: MutableList<ViewProperty> = Lists.newArrayList()
        val childrenProps: MutableMap<String, Any> = Maps.newHashMap()
        for (p in viewProperties.entries) {
            val fullName = getPropertyName(p.key)!!
            val value = p.value
            if (fullName.startsWith("$META_KEY:$CHILD_KEY")) {
                childrenProps[fullName] = value
            } else {
                val property = ViewPropertyParser.parse(
                    fullName,
                    value.toString()
                )
                properties.add(property)
                namedProperties[property.name] = property
            }
        }
        node.namedProperties.putAll(namedProperties)
        node.properties.addAll(properties)
        node.properties.map { node.addPropertyToGroup(it) }
        node.namedProperties["id"]?.let {
            node.id = it.value
        }
        // hide meta props
        val metaProps = node.groupedProperties.remove(META_KEY)
        addChildren(node, metaProps!!, childrenProps)
    }
    private fun addChildren(
        parent: ViewNode,
        metaProps: MutableList<ViewProperty>,
        childrenProps: Map<String, Any>
    ) {
        // no children if there is no matching prop
        val childCountProp = metaProps.find { it.name == CHILD_COUNT_KEY } ?: return
        val childCount = childCountProp.value.toInt()
        val children = childrenProps.entries.sortedBy { getChildIndex(it.key) }.map { createViewNode(it.value as Map<Short, Any>, parent) }
        verify(childCount == children.size, String.format(
            "Expect view node %s to have %d children but instead found %d",
            parent, childCount, children.size
        ))
        parent.children.addAll(children)
    }
}