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

import com.android.layoutinspector.ProtocolVersion
import com.android.layoutinspector.model.ViewNode
import com.google.common.base.Charsets
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.util.*
import java.util.function.BiConsumer
import java.util.function.BinaryOperator
import java.util.function.Function
import java.util.function.Supplier
import java.util.stream.Collector

object ViewNodeParser {
    /** Parses the flat string representation of a view node and returns the root node.  */
    @Throws(IOException::class, StringIndexOutOfBoundsException::class)
    @JvmStatic
    @JvmOverloads
    fun parse(
        bytes: ByteArray,
        version: ProtocolVersion = ProtocolVersion.Version1,
        skippedProperties: Collection<String> = Collections.emptyList()
    ): ViewNode? {
        return when (version) {
            ProtocolVersion.Version1 -> parseV1ViewNode(bytes, skippedProperties)
            ProtocolVersion.Version2 -> parseV2ViewNode(bytes)
        }
    }

    private fun parseV2ViewNode(bytes: ByteArray): ViewNode? {
        return ViewNodeV2Parser().parse(bytes)
    }

    private fun parseV1ViewNode(
        bytes: ByteArray,
        skippedProperties: Collection<String>
    ): ViewNode? {
        var root: ViewNode? = null
        var lastNode: ViewNode? = null
        var lastWhitespaceCount = Integer.MIN_VALUE
        val stack = Stack<ViewNode>()
        val input = BufferedReader(
            InputStreamReader(ByteArrayInputStream(bytes), Charsets.UTF_8)
        )
        for (line in input.lines().collect(MergeNewLineCollector)) {
            if ("DONE.".equals(line, ignoreCase = true)) {
                break
            }
            // determine parent through the level of nesting by counting whitespaces
            var whitespaceCount = 0
            while (line[whitespaceCount] == ' ') {
                whitespaceCount++
            }
            if (lastWhitespaceCount < whitespaceCount) {
                stack.push(lastNode)
            } else if (!stack.isEmpty()) {
                val count = lastWhitespaceCount - whitespaceCount
                for (i in 0 until count) {
                    stack.pop()
                }
            }
            lastWhitespaceCount = whitespaceCount
            var parent: ViewNode? = null
            if (!stack.isEmpty()) {
                parent = stack.peek()
            }
            lastNode = createViewNode(parent, line.trim(), skippedProperties)
            if (root == null) {
                root = lastNode
            }
        }
        root?.updateNodeDrawn(true)
        return root
    }

    private fun createViewNode(
        parent: ViewNode?,
        data: String,
        skippedProperties: Collection<String>
    ): ViewNode {
        var data = data
        var delimIndex = data.indexOf('@')
        if (delimIndex < 0) {
            throw IllegalArgumentException("Invalid format for ViewNode, missing @: $data")
        }
        val name = data.substring(0, delimIndex)
        data = data.substring(delimIndex + 1)
        delimIndex = data.indexOf(' ')
        val hash = data.substring(0, delimIndex)
        val node = ViewNode(parent, name, hash)
        node.index = parent?.children?.size ?: 0
        if (data.length > delimIndex + 1) {
            loadProperties(node, data.substring(delimIndex + 1), skippedProperties)
            node.id = node.getProperty("mID", "id")?.value
        }
        node.displayInfo = DisplayInfoFactory.createDisplayInfoFromNode(node)
        parent?.let {
            it.children.add(node)
        }
        return node
    }

    private fun loadProperties(
        node: ViewNode,
        data: String,
        skippedProperties: Collection<String>
    ) {
        var start = 0
        var stop: Boolean
        do {
            val index = data.indexOf('=', start)
            val fullName = data.substring(start, index)
            val index2 = data.indexOf(',', index + 1)
            val length = Integer.parseInt(data.substring(index + 1, index2))
            start = index2 + 1 + length
            if (!skippedProperties.contains(fullName)) {
                val value = data.substring(index2 + 1, index2 + 1 + length)
                val property = ViewPropertyParser.parse(fullName, value)
                node.properties.add(property)
                node.namedProperties[property.fullName] = property
                node.addPropertyToGroup(property)
            }
            stop = start >= data.length
            if (!stop) {
                start += 1
            }
        } while (!stop)
        node.properties.sort()
    }

    /**
     * A custom collector that handles a special case see b/79183623
     * If a text field has text containing a new line it'll cause the view node output to be split
     * across multiple lines so the collector processes the file output and merges those back into a
     * single line so we can correctly create view nodes.
     */
    private object MergeNewLineCollector : Collector<String, MutableList<String>, List<String>> {
        override fun characteristics(): Set<Collector.Characteristics> {
            return setOf(Collector.Characteristics.CONCURRENT)
        }

        override fun supplier() = Supplier<MutableList<String>> { ArrayList() }
        override fun finisher() = Function<MutableList<String>, List<String>> { it.toList() }
        override fun combiner() =
            BinaryOperator<MutableList<String>> { t, u -> t.apply { addAll(u) } }

        override fun accumulator() = BiConsumer<MutableList<String>, String> { stringGroup, line ->
            val newLine = line.trim()
            // add the original line because we need to keep the spacing to determine hierarchy
            if (newLine.startsWith("\\n")) {
                stringGroup[stringGroup.lastIndex] = stringGroup.last() + line
            } else {
                stringGroup.add(line)
            }
        }
    }
}