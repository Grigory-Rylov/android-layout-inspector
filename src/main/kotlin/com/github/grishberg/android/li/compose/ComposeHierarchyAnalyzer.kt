package com.github.grishberg.android.li.compose

import com.github.grishberg.android.li.compose.model.ComposeCandidateNode
import com.github.grishberg.android.li.compose.model.ComposeHierarchySummary
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import javax.xml.parsers.SAXParserFactory

/**
 * Parses a `uiautomator dump` XML and extracts a Compose-focused summary.
 *
 * Heuristic for "Compose candidate":
 *  - node has any Compose-related ancestor class
 *    (`androidx.compose.ui.platform.AndroidComposeView` or `ComposeView`)
 *  - OR node is `android.view.View` with empty `resource-id` and a non-empty
 *    `text` or `content-desc` (typical for Compose semantics nodes exposed
 *    through accessibility).
 */
class ComposeHierarchyAnalyzer {

    private val boundsPattern = "\\[(-?\\d+),(-?\\d+)]\\[(-?\\d+),(-?\\d+)]".toRegex()

    fun analyze(deviceName: String, xmlDump: String): Result {
        val root = parseRoot(xmlDump)
            ?: return Result(
                ComposeHierarchySummary(
                    deviceName = deviceName,
                    packageName = "<unknown>",
                    totalNodes = 0,
                    visibleNodes = 0,
                    composeRootCount = 0,
                    composeCandidateCount = 0,
                    textNodeCount = 0,
                    contentDescNodeCount = 0,
                    resourceIdNodeCount = 0,
                    classDistribution = emptyList(),
                    topTexts = emptyList(),
                    topContentDescriptions = emptyList(),
                ),
                null,
            )

        val counters = Counters()
        walkAndCount(root, false, counters)

        val topPackage = counters.packageCounts.entries.maxByOrNull { it.value }?.key.orEmpty()
        val classDistribution = counters.classCounts.entries
            .sortedByDescending { it.value }
            .take(15)
            .map { ComposeHierarchySummary.ClassCount(it.key, it.value) }
        val topTexts = counters.textCounts.entries
            .sortedByDescending { it.value }
            .take(30)
            .map { "${it.key} (×${it.value})" }
        val topContentDescriptions = counters.descCounts.entries
            .sortedByDescending { it.value }
            .take(30)
            .map { "${it.key} (×${it.value})" }

        val summary = ComposeHierarchySummary(
            deviceName = deviceName,
            packageName = topPackage,
            totalNodes = counters.total,
            visibleNodes = counters.visible,
            composeRootCount = counters.composeRoots,
            composeCandidateCount = counters.composeCandidates,
            textNodeCount = counters.textNodes,
            contentDescNodeCount = counters.descNodes,
            resourceIdNodeCount = counters.idNodes,
            classDistribution = classDistribution,
            topTexts = topTexts,
            topContentDescriptions = topContentDescriptions,
        )
        return Result(summary, root.toTreeNode())
    }

    private fun parseRoot(xml: String): RawNode? {
        val factory = SAXParserFactory.newInstance()
        val handler = NodeHandler()
        val stream = ByteArrayInputStream(xml.toByteArray(StandardCharsets.UTF_8))
        factory.newSAXParser().parse(stream, handler)
        return handler.root
    }

    private fun walkAndCount(node: RawNode, hasComposeAncestor: Boolean, c: Counters) {
        c.total++
        val visible = node.width > 0 && node.height > 0
        if (visible) c.visible++
        node.className.takeIf { it.isNotEmpty() }?.let {
            c.classCounts.merge(it, 1, Int::plus)
        }
        node.pkg.takeIf { it.isNotEmpty() && it != "<unknown>" }?.let {
            c.packageCounts.merge(it, 1, Int::plus)
        }
        node.text?.takeIf { it.isNotEmpty() }?.let {
            c.textNodes++
            c.textCounts.merge(it, 1, Int::plus)
        }
        node.contentDescription?.takeIf { it.isNotEmpty() }?.let {
            c.descNodes++
            c.descCounts.merge(it, 1, Int::plus)
        }
        node.resourceId?.takeIf { it.isNotEmpty() }?.let { c.idNodes++ }

        val isComposeRoot = isComposeRootClass(node.className)
        val childHasComposeAncestor = hasComposeAncestor || isComposeRoot
        if (isComposeRoot) c.composeRoots++

        node.isComposeCandidate =
            hasComposeAncestor || isComposeRoot || isComposeSemanticsCandidate(node)
        if (node.isComposeCandidate) c.composeCandidates++

        node.children.forEach { walkAndCount(it, childHasComposeAncestor, c) }
    }

    private fun isComposeRootClass(className: String): Boolean {
        return className == "androidx.compose.ui.platform.ComposeView" ||
            className == "androidx.compose.ui.platform.AndroidComposeView" ||
            className.endsWith(".AndroidComposeView") ||
            className.endsWith(".ComposeView")
    }

    private fun isComposeSemanticsCandidate(node: RawNode): Boolean {
        if (node.className != "android.view.View") return false
        if (!node.resourceId.isNullOrEmpty()) return false
        return !node.text.isNullOrEmpty() || !node.contentDescription.isNullOrEmpty()
    }

    private fun parseBounds(s: String?): IntArray {
        if (s.isNullOrEmpty()) return intArrayOf(0, 0, 0, 0)
        val m = boundsPattern.find(s) ?: return intArrayOf(0, 0, 0, 0)
        return intArrayOf(
            m.groupValues[1].toInt(),
            m.groupValues[2].toInt(),
            m.groupValues[3].toInt(),
            m.groupValues[4].toInt(),
        )
    }

    private fun parseId(raw: String?): String? {
        if (raw.isNullOrEmpty()) return null
        val pos = raw.lastIndexOf(":id/")
        return if (pos < 0) raw else raw.substring(pos + 4)
    }

    private inner class NodeHandler : DefaultHandler() {
        var root: RawNode? = null
        private val stack = ArrayDeque<RawNode>()

        override fun startElement(uri: String, localName: String?, qName: String, attributes: Attributes) {
            if (qName != "node") return
            val bounds = parseBounds(attributes.getValue("bounds"))
            val node = RawNode(
                className = attributes.getValue("class").orEmpty(),
                pkg = attributes.getValue("package").orEmpty(),
                resourceId = parseId(attributes.getValue("resource-id")),
                text = attributes.getValue("text"),
                contentDescription = attributes.getValue("content-desc"),
                left = bounds[0],
                top = bounds[1],
                right = bounds[2],
                bottom = bounds[3],
            )
            stack.lastOrNull()?.children?.add(node)
            stack.addLast(node)
            if (root == null) root = node
        }

        override fun endElement(uri: String, localName: String, qName: String) {
            if (qName == "node") stack.removeLast()
        }
    }

    private class RawNode(
        val className: String,
        val pkg: String,
        val resourceId: String?,
        val text: String?,
        val contentDescription: String?,
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int,
    ) {
        val children = mutableListOf<RawNode>()
        val width: Int get() = right - left
        val height: Int get() = bottom - top
        var isComposeCandidate: Boolean = false

        fun toTreeNode(parent: ComposeCandidateNode? = null): ComposeCandidateNode {
            val node = ComposeCandidateNode(
                className = className,
                resourceId = resourceId,
                text = text,
                contentDescription = contentDescription,
                left = left,
                top = top,
                right = right,
                bottom = bottom,
                isComposeCandidate = isComposeCandidate,
                parentNode = parent,
            )
            for (child in children) {
                node.add(child.toTreeNode(node))
            }
            return node
        }
    }

    private class Counters {
        var total = 0
        var visible = 0
        var composeRoots = 0
        var composeCandidates = 0
        var textNodes = 0
        var descNodes = 0
        var idNodes = 0
        val classCounts = LinkedHashMap<String, Int>()
        val packageCounts = LinkedHashMap<String, Int>()
        val textCounts = LinkedHashMap<String, Int>()
        val descCounts = LinkedHashMap<String, Int>()
    }

    data class Result(
        val summary: ComposeHierarchySummary,
        val root: ComposeCandidateNode?,
    )
}
