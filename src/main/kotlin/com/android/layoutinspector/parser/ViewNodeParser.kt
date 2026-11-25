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
import com.android.layoutinspector.model.ViewProperty
import com.google.common.base.Charsets
import com.google.common.collect.Lists
import com.google.common.collect.Maps
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.util.ArrayList
import java.util.Collections
import java.util.Stack
import java.util.function.BiConsumer
import java.util.function.BinaryOperator
import java.util.function.Function
import java.util.function.Supplier
import java.util.stream.Collector

object ViewNodeParser {

    /**
     * A line that starts a node: `<indent>Name@hash[ props]`. Anything else seen mid-stream is
     * a fragment of a property value that contained an unescaped line break (see
     * [MergeNewLineCollector]).
     */
    private val NODE_LINE = Regex("""^\s*[^\s@]+@\S+( .*)?$""")
    private const val DONE_LINE = "DONE."

    /** Optional sink for per-line parse failures; the bridge wires it to the plugin logger. */
    @Volatile
    @JvmStatic
    var lineErrorLogger: ((String) -> Unit)? = null
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

    /**
     * True when the failure chain is a corrupt/garbled line (bad offsets or numbers): such a
     * line must be skipped, not kill the whole capture.
     */
    private fun isCorruptLine(t: Throwable): Boolean =
        t is IndexOutOfBoundsException ||
            t is NumberFormatException ||
            t is IllegalArgumentException

    private fun parseV1ViewNode(
        bytes: ByteArray,
        skippedProperties: Collection<String>
    ): ViewNode? {
        var root: ViewNode? = null
        var lastNode: ViewNode? = null
        var lastWhitespaceCount = Integer.MIN_VALUE
        val stack = Stack<ViewNode>()
        // The device writes each property as `<name>=<byteLength>,<utf8Value>` separated by `:`,
        // so offsets are BYTE offsets. Decoding the stream byte-per-char (ISO-8859-1) keeps the
        // parser's char indices aligned with them; individual values are UTF-8 decoded in
        // [loadPropertiesAndCreateNode]. Decoding the whole stream as UTF-8 used to shift every
        // offset after any non-ASCII value (Cyrillic text) and crashed substring bounds.
        val input = BufferedReader(
            InputStreamReader(ByteArrayInputStream(bytes), Charsets.ISO_8859_1)
        )
        for (line in input.lines().collect(MergeNewLineCollector)) {
            if (DONE_LINE.equals(line, ignoreCase = true)) {
                break
            }
            if (line.isEmpty()) {
                continue
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
            // One corrupt line (device escapes only '\n'; values may still carry '\r', and
            // stream glitches happen) must not kill the whole capture - skip the node and keep
            // its children attached one level up.
            val node = try {
                createViewNode(parent, line.trim(), skippedProperties)
            } catch (e: Exception) {
                if (!isCorruptLine(e)) throw e
                lineErrorLogger?.invoke(
                    "YALI: skipping corrupt hierarchy line: " +
                        "\"${line.take(160)}\" (${e.javaClass.simpleName}: ${e.message})"
                )
                continue
            }
            lastNode = node
            if (root == null) {
                root = node
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
        if (delimIndex < 0) {
            // "Name@hash" with no properties after the hash
            val hash = data
            val node = ViewNode(parent, name, hash)
            node.index = parent?.children?.size ?: 0
            parent?.let { it.children.add(node) }
            return node
        }
        val hash = data.substring(0, delimIndex)

        val node = if (data.length > delimIndex + 1) {
            loadPropertiesAndCreateNode(parent, name, hash, data.substring(delimIndex + 1), skippedProperties)
        } else {
            ViewNode(parent, name, hash)
        }
        node.index = parent?.children?.size ?: 0
        parent?.let {
            it.children.add(node)
        }
        return node
    }

    private fun loadPropertiesAndCreateNode(
        parent: ViewNode?,
        name: String,
        hash: String,
        data: String,
        skippedProperties: Collection<String>
    ): ViewNode {
        var start = 0
        var stop: Boolean
        val namedProperties: MutableMap<String, ViewProperty> = Maps.newHashMap()
        val properties: MutableList<ViewProperty> = Lists.newArrayList()

        do {
            val index = data.indexOf('=', start)
            val fullName = data.substring(start, index)
            val index2 = data.indexOf(',', index + 1)
            val length = Integer.parseInt(data.substring(index + 1, index2))
            start = index2 + 1 + length
            if (!skippedProperties.contains(fullName)) {
                // `value` is the raw byte-per-char window written by the device; non-ASCII
                // values (e.g. Cyrillic text) must be re-decoded from UTF-8 to be readable.
                val raw = data.substring(index2 + 1, index2 + 1 + length)
                val value = if (raw.any { it.code > 0x7F }) {
                    String(raw.toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8)
                } else {
                    raw
                }
                val property = ViewPropertyParser.parse(fullName, value)
                properties.add(property)
                namedProperties[property.fullName] = property
                // TODO: check it's ok for old parser
                //node.addPropertyToGroup(property)
            }
            stop = start >= data.length
            if (!stop) {
                start += 1
            }
        } while (!stop)
        properties.sort()
        val node = ViewNode(
            parent, name, hash, namedProperties = namedProperties,
            properties = properties,
            displayInfo = DisplayInfoFactory.createDisplayInfoFromNamedProperties(namedProperties),
        )
        node.id = node.getProperty("mID", "id")?.value

        return node
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
            } else if (
                stringGroup.isNotEmpty() &&
                !DONE_LINE.equals(line, ignoreCase = true) &&
                !NODE_LINE.matches(line)
            ) {
                // A property value contained an unescaped line break the device did not mark
                // (e.g. a raw '\r' inside text): the rest of the value starts here and is NOT a
                // node line. Re-join it with the newline readLine() consumed, so the declared
                // byte lengths keep their offsets.
                stringGroup[stringGroup.lastIndex] = stringGroup.last() + "\n" + line
            } else {
                stringGroup.add(line)
            }
        }
    }
}