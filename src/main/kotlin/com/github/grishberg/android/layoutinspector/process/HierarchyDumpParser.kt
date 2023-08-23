package com.github.grishberg.android.layoutinspector.process

import com.github.grishberg.android.layoutinspector.domain.AbstractViewNode
import com.github.grishberg.android.layoutinspector.domain.DumpViewNode
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import javax.xml.parsers.SAXParser
import javax.xml.parsers.SAXParserFactory

class HierarchyDumpParser {

    private val BOUNDS_PATTERN = "\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]".toRegex()
    fun parseDump(viewDump: String): AbstractViewNode? {
        val factory = SAXParserFactory.newInstance()
        val saxParser: SAXParser = factory.newSAXParser()
        val handler = ViewDumpHandler()

        val stream: InputStream = ByteArrayInputStream(viewDump.toByteArray(StandardCharsets.UTF_8))
        saxParser.parse(stream, handler)
        return handler.rootNode
    }

    private inner class ViewDumpHandler : DefaultHandler() {
        var state: State? = null

        var rootNode: DumpViewNode? = null

        override fun startElement(uri: String, localName: String?, qName: String, attributes: Attributes) {
            state = when (qName) {
                "hierarchy" -> {
                    HierarchyState()
                }

                "node" -> {
                    val nodeState = NodeState(rootNode)
                    nodeState.processAttributes(attributes)
                    rootNode = nodeState.createNode()
                    nodeState

                }

                else -> throw IllegalStateException()
            }
        }

        override fun endElement(uri: String, localName: String, qName: String) {
            state?.endElement(uri, localName, qName)

            if (qName == "node") {
                rootNode?.parent?.let {
                    rootNode = it
                }
            }
        }

        override fun characters(ch: CharArray, start: Int, length: Int) {
            state?.characters(ch, start, length)
        }
    }

    private interface State {
        fun processAttributes(attributes: Attributes)

        fun characters(ch: CharArray, start: Int, length: Int)

        fun endElement(uri: String, localName: String, qName: String)
    }

    private inner class HierarchyState : State {

        override fun processAttributes(attributes: Attributes) = Unit

        override fun characters(ch: CharArray, start: Int, length: Int) = Unit
        override fun endElement(uri: String, localName: String, qName: String) = Unit
    }

    private inner class NodeState(
        private val parentNode: DumpViewNode?
    ) : State {

        private lateinit var newNode: DumpViewNode

        override fun processAttributes(attributes: Attributes) {
            val pkg = attributes.getValue("package")
            val className = attributes.getValue("class")
            val id = attributes.getValue("resource-id")
            val globalBounds = attributes.getValue("bounds")
            val rectBounds = parseBounds(globalBounds)
            newNode = DumpViewNode(
                parent = parentNode, pkg = pkg, name = className, id = parseId(id),
                rectBounds.left, rectBounds.top, rectBounds.right, rectBounds.bottom, attributes.getValue("text")
            )
            parentNode?.addChild(newNode)
        }

        private fun parseId(id: String?): String? {
            if (id == null) {
                return null
            }

            val pos = id.lastIndexOf(":id/")
            if (pos < 0) {
                return id
            }
            return id.substring(pos + 1)
        }

        private fun parseBounds(bounds: String): Rect {
            val resultMatch = BOUNDS_PATTERN.find(bounds) ?: return Rect(0, 0, 0, 0)
            return Rect(
                resultMatch.groupValues[1].toInt(),
                resultMatch.groupValues[2].toInt(),
                resultMatch.groupValues[3].toInt(),
                resultMatch.groupValues[4].toInt()
            )
        }

        override fun characters(ch: CharArray, start: Int, length: Int) {
            println(ch)
        }

        override fun endElement(uri: String, localName: String, qName: String) {
            println(qName)
        }

        fun createNode(): DumpViewNode {
            return newNode
        }
    }

    private data class Rect(val left: Int, val top: Int, val right: Int, val bottom: Int)
}
