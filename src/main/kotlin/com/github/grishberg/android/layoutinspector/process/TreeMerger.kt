package com.github.grishberg.android.layoutinspector.process

import com.android.layoutinspector.common.AppLogger
import com.android.layoutinspector.model.ViewNode
import com.github.grishberg.android.layoutinspector.domain.AbstractViewNode
import com.github.grishberg.android.layoutinspector.domain.ComposeViewNode
import com.github.grishberg.android.layoutinspector.domain.DumpViewNode

private const val COMPOSE_FULL_NAME = "androidx.compose.ui.platform.ComposeView"
private const val TAG = "TreeMerger"

class TreeMerger(private val logger: AppLogger) {

    fun mergeNodes(layoutRootNode: AbstractViewNode, dumpViewNode: DumpViewNode): AbstractViewNode {
        logger.d("YALI TreeMerger: try to merge")
        val layoutPath = mutableListOf<NodePath>()
        extractAllComposeNodes(layoutRootNode, layoutPath)

        val dumpPath = mutableListOf<NodePath>()
        extractAllComposeNodes(dumpViewNode, dumpPath)

        if (layoutPath.isEmpty() || dumpPath.isEmpty()) {
            logger.w("YALI TreeMerger: ComposeView is not found")
            return layoutRootNode
        }

        for (path in layoutPath) {
            val suitableNodePath = dumpPath.firstOrNull() { it == path } ?: continue
            val layoutComposeNode = path.targetNode
            if (layoutComposeNode is ViewNode) {
                logger.w("YALI TreeMerger: ComposeView is found and replaced size=${suitableNodePath.targetNode.width} x ${suitableNodePath.targetNode.height}")
                layoutComposeNode.replaceChildren(suitableNodePath.targetNode.children)
            }
        }

        return layoutRootNode
    }

    private fun extractAllComposeNodes(root: AbstractViewNode, paths: MutableList<NodePath>) {
        if (root.name == COMPOSE_FULL_NAME) {
            paths.add(NodePath(root))
            return
        }
        for (child in root.children) {
            extractAllComposeNodes(child, paths)
        }
    }

    fun mergeComposeNodes(layoutNode: ViewNode, composeNodes: List<ComposeViewNode>): ViewNode {
        logger.d("$TAG: mergeComposeNodes() layoutNode = $layoutNode, composeNodes size = ${composeNodes.size}")
        val result = layoutNode.clone()
        val layoutNodeChildren = layoutNode.children

        if (composeNodes.isEmpty() || layoutNodeChildren.isEmpty()) {
            return result
        }

        // Создаем карту для быстрого поиска Compose узлов по их ID
        val composeNodeMap = composeNodes.associateBy { it.drawId }

        for (i in layoutNodeChildren.indices) {
            val layoutChild = layoutNodeChildren[i]
            val composeChild = composeNodeMap[layoutChild.id]
            if (composeChild != null) {
                // Если нашли соответствующий Compose узел, заменяем обычный узел на Compose
                result.children[i] = composeChild
                // Рекурсивно обрабатываем дочерние узлы
                if (layoutChild.children.isNotEmpty() && composeChild.children.isNotEmpty()) {
                    val mergedChild = mergeComposeNodes(layoutChild, composeChild.children.toList())
                    result.children[i] = mergedChild
                }
            }
        }
        return result
    }

    private fun findDumpChild(layoutChild: ViewNode, dumpChildren: List<DumpViewNode>): DumpViewNode? {
        for (dumpChild in dumpChildren) {
            if (isSameNode(layoutChild, dumpChild)) {
                return dumpChild
            }
        }
        return null
    }

    private fun isSameNode(layoutChild: ViewNode, dumpChild: DumpViewNode): Boolean {
        return layoutChild.id == dumpChild.id
    }

    private class NodePath(
        val targetNode: AbstractViewNode,
    ) {

        private val path: List<AbstractViewNode>

        init {
            path = cratePathToRoot(targetNode)
        }

        override fun equals(other: Any?): Boolean {
            if (other !is NodePath) {
                return false
            }
            if (targetNode.name != other.targetNode.name) {
                return false
            }
            if (!isIdSame(targetNode.id, other.targetNode.id)) {
                return false
            }
            if (targetNode.locationOnScreenX != other.targetNode.locationOnScreenX || targetNode.locationOnScreenY != other.targetNode.locationOnScreenY /*|| targetNode.width != other.targetNode.width || targetNode.height != other.targetNode.height*/) {
                return false
            }
            if (other.path.size != path.size) {
                return false
            }

            for (i in path.indices) {
                val thisId = path[i].id
                val otherId = other.path[i].id
                if (!isIdSame(thisId, otherId)) {
                    return false
                }
            }

            return true
        }

        private fun isIdSame(thisId: String?, otherId: String?): Boolean {
            if ((thisId == null || thisId == "" || thisId == "NO_ID") && (otherId == null || otherId == "" || otherId == "NO_ID")) {
                return true
            }
            return thisId == otherId
        }

        override fun hashCode(): Int {
            return targetNode.name.hashCode() + path.hashCode()
        }

        private fun cratePathToRoot(targetNode: AbstractViewNode): List<AbstractViewNode> {
            val result = mutableListOf<AbstractViewNode>()
            if (targetNode.parent == null) {
                return emptyList()
            }

            var parent = targetNode.parent as AbstractViewNode?
            while (parent != null) {
                result.add(parent)
                parent = parent.parent as AbstractViewNode?
            }
            return result

        }

    }
}
