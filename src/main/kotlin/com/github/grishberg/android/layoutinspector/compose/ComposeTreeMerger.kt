package com.github.grishberg.android.layoutinspector.compose

import com.android.layoutinspector.common.AppLogger
import com.android.layoutinspector.model.DisplayInfo
import com.android.layoutinspector.model.ViewNode
import com.android.layoutinspector.model.ViewProperty
import com.github.grishberg.android.layoutinspector.compose.ComposeInspectionCodec.ComposeNode
import com.github.grishberg.android.layoutinspector.domain.AbstractViewNode
import com.github.grishberg.android.layoutinspector.domain.DumpViewNode
import com.github.grishberg.android.layoutinspector.compose.ComposeInspectionCodec.ComposableRoot
import com.github.grishberg.android.layoutinspector.compose.ComposeInspectionCodec.ComposeRect

/**
 * Grafts Compose hierarchies (captured through Android Studio's compose inspector) under the
 * ComposeView / AndroidComposeView nodes of the JDWP-captured [ViewNode] tree, analogously to
 * [com.github.grishberg.android.layoutinspector.process.TreeMerger] for uiautomator dumps.
 *
 * Strategy:
 *  - collect all candidate host nodes (`*ComposeView` / `AndroidComposeView`), keeping only the
 *    deepest ones when one candidate nests inside another (ComposeView -> AndroidComposeView);
 *  - single host: all compose roots are attached there (fast path);
 *  - several hosts: each ComposableRoot is attached to the host whose screen bounds overlap the
 *    root's bounds most (both are absolute screen coordinates — the compose agent offsets node
 *    bounds by the window root view's on-screen position before serializing);
 *  - roots without any usable host are appended under the decor root as synthetic nodes named
 *    `Compose root view <view id>`, anchored at the decor root's screen position.
 *
 * The merged compose children exist in memory only; `.li` files keep persisting the plain JDWP
 * tree (same as the dump-merge mode today).
 */
class ComposeTreeMerger(private val logger: AppLogger) {

    companion object {
        private val COMPOSE_HOST_SIMPLE_NAMES = setOf("ComposeView", "AndroidComposeView")
    }

    fun mergeNodes(layoutRootNode: AbstractViewNode, roots: List<ComposableRoot>): AbstractViewNode {
        if (roots.isEmpty()) {
            logger.d("YALI ComposeTreeMerger: no compose roots")
            return layoutRootNode
        }
        val hosts = collectHosts(layoutRootNode).toMutableList()
        // when one host is nested inside another (ComposeView contains AndroidComposeView),
        // only the innermost host is usable - attaching to both would duplicate the subtree
        hosts.removeAll { outer -> hosts.any { inner -> inner !== outer && isDescendant(inner, outer) } }
        logger.d("YALI ComposeTreeMerger: ${roots.size} compose root(s), ${hosts.size} compose host(s)")

        for (root in roots) {
            val host = when {
                hosts.isEmpty() -> null
                hosts.size == 1 -> hosts.first()
                else -> bestHostMatch(root, hosts)
            }
            if (host != null) {
                attachUnder(host, root)
            } else {
                appendSyntheticUnderDecor(layoutRootNode, root)
            }
        }
        return layoutRootNode
    }

    private fun attachUnder(host: AbstractViewNode, root: ComposableRoot) {
        ComposeNodeAdapter.adaptRootNodes(root.nodes, host).forEach { addChildTo(host, it) }
        logger.d(
            "YALI ComposeTreeMerger: grafted view_id=${root.viewId} " +
                "(${root.nodes.size} root composables) under ${host.name}"
        )
    }

    private fun appendSyntheticUnderDecor(decorRoot: AbstractViewNode, root: ComposableRoot) {
        // compose bounds are already absolute screen pixels (see ComposeNodeAdapter doc)
        val rootBounds = unionBounds(root.nodes)
        val originX = decorRoot.locationOnScreenX
        val originY = decorRoot.locationOnScreenY
        val absX = rootBounds.left
        val absY = rootBounds.top
        val properties = listOf(
            ViewProperty(
                "layout:getLocationOnScreen_x()", "getLocationOnScreen_x()", "layout",
                absX.toString(), false, absX,
            ),
            ViewProperty(
                "layout:getLocationOnScreen_y()", "getLocationOnScreen_y()", "layout",
                absY.toString(), false, absY,
            ),
            ViewProperty("compose:view_id", "view_id", "compose", root.viewId.toString()),
        )
        val syntheticHost = ViewNode(
            parent = decorRoot as? ViewNode,
            name = "Compose root view ${root.viewId}",
            hash = "compose-root-${root.viewId}",
            namedProperties = properties.associateBy { it.fullName },
            properties = properties,
            children = mutableListOf(),
            displayInfo = DisplayInfo(
                false, true,
                absX - originX, absY - originY,
                rootBounds.width, rootBounds.height,
                0, 0, true, 0f, 0f, 0f, 0f, null,
            ),
        )
        syntheticHost.children.addAll(ComposeNodeAdapter.adaptRootNodes(root.nodes, syntheticHost))
        addChildTo(decorRoot, syntheticHost)
        logger.d(
            "YALI ComposeTreeMerger: no compose host found, appended " +
                "'${syntheticHost.name}' under ${decorRoot.name}"
        )
    }

    /**
     * Both the compose root bounds and the host node bounds are absolute screen pixels; the host
     * overlapping the largest fraction of the root wins, ties go to the tighter (smaller) host.
     */
    private fun bestHostMatch(root: ComposableRoot, hosts: List<AbstractViewNode>): AbstractViewNode {
        val rootBounds = unionBounds(root.nodes)
        if (rootBounds.width <= 0 || rootBounds.height <= 0) {
            logger.w("YALI ComposeTreeMerger: root ${root.viewId} has empty bounds, using first host")
            return hosts.first()
        }
        val rootArea = rootBounds.width.toLong() * rootBounds.height.toLong()
        var best: AbstractViewNode? = null
        var bestScore = 0.0
        var bestArea = Long.MAX_VALUE
        for (host in hosts) {
            val overlapW = (minOf(rootBounds.right, host.locationOnScreenX + host.width) -
                maxOf(rootBounds.left, host.locationOnScreenX)).coerceAtLeast(0).toLong()
            val overlapH = (minOf(rootBounds.bottom, host.locationOnScreenY + host.height) -
                maxOf(rootBounds.top, host.locationOnScreenY)).coerceAtLeast(0).toLong()
            val score = (overlapW * overlapH).toDouble() / rootArea
            val hostArea = host.width.toLong() * host.height.toLong()
            if (score > bestScore || (score == bestScore && hostArea < bestArea)) {
                bestScore = score
                bestArea = hostArea
                best = host
            }
        }
        return best ?: hosts.first()
    }

    private fun unionBounds(nodes: List<ComposeNode>): ComposeRect {
        if (nodes.isEmpty()) {
            return ComposeRect(0, 0, 0, 0)
        }
        var left = Int.MAX_VALUE
        var top = Int.MAX_VALUE
        var right = Int.MIN_VALUE
        var bottom = Int.MIN_VALUE
        for (node in nodes) {
            left = minOf(left, node.bounds.left)
            top = minOf(top, node.bounds.top)
            right = maxOf(right, node.bounds.right)
            bottom = maxOf(bottom, node.bounds.bottom)
        }
        if (left > right || top > bottom) {
            return ComposeRect(0, 0, 0, 0)
        }
        return ComposeRect(left, top, right - left, bottom - top)
    }

    private fun collectHosts(node: AbstractViewNode): List<AbstractViewNode> {
        val result = mutableListOf<AbstractViewNode>()
        collectHosts(node, result)
        return result
    }

    private fun collectHosts(node: AbstractViewNode, result: MutableList<AbstractViewNode>) {
        if (COMPOSE_HOST_SIMPLE_NAMES.contains(node.typeAsString)) {
            result.add(node)
            // AndroidComposeView is usually the only meaningful descendant; still keep scanning
        }
        node.children.forEach { collectHosts(it, result) }
    }

    /** [AbstractViewNode.children] is typed as read-only List; concrete nodes own mutable storage. */
    private fun addChildTo(parent: AbstractViewNode, child: AbstractViewNode) {
        when (parent) {
            is ViewNode -> parent.children.add(child)
            is DumpViewNode -> parent.addChild(child)
            else -> @Suppress("UNCHECKED_CAST") {
                (parent.children as MutableList<AbstractViewNode>).add(child)
            }
        }
    }

    private fun isDescendant(candidate: AbstractViewNode, ancestor: AbstractViewNode): Boolean {
        var parent = candidate.parent
        while (parent != null) {
            if (parent === ancestor) {
                return true
            }
            parent = (parent as? AbstractViewNode)?.parent
        }
        return false
    }
}
