package com.github.grishberg.android.layoutinspector.process

import com.android.layoutinspector.model.ViewNode
import com.github.grishberg.android.layoutinspector.domain.AbstractViewNode
import com.github.grishberg.android.layoutinspector.domain.DumpViewNode

private const val COMPOSE_FULL_NAME = "androidx.compose.ui.platform.ComposeView"

class TreeMerger {

    fun mergeNodes(layoutRootNode: AbstractViewNode, dumpViewNode: DumpViewNode): AbstractViewNode {
        val layoutPath = mutableListOf<NodePath>()
        walk(layoutRootNode, layoutPath)

        val dumpPath = mutableListOf<NodePath>()
        walk(dumpViewNode, dumpPath)

        if (layoutPath.isEmpty() || dumpPath.isEmpty()) {
            return layoutRootNode
        }

        for (path in layoutPath) {
            val suitableNodePath =  dumpPath.firstOrNull() { it == path } ?: continue
            val layoutComposeNode = path.targetNode
            if (layoutComposeNode is ViewNode) {
                layoutComposeNode.replaceChildren(suitableNodePath.targetNode.children)
            }
        }

        return layoutRootNode
    }

    private fun walk(root: AbstractViewNode, paths: MutableList<NodePath>) {
        if (root.name == COMPOSE_FULL_NAME) {
            paths.add(NodePath(root))
            return
        }
        for (child in root.children) {
            walk(child, paths)
        }
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
            if (targetNode.id != other.targetNode.id) {
                return false
            }
            if (targetNode.locationOnScreenX != other.targetNode.locationOnScreenX || targetNode.locationOnScreenY != other.targetNode.locationOnScreenY || targetNode.width != other.targetNode.width || targetNode.height != other.targetNode.height) {
                return false
            }
            if (other.path.size != path.size) {
                return false
            }

            for (i in path.indices) {
                val thisId = path[i].id
                val otherId = other.path[i].id
                if ((thisId == null || thisId == "" || thisId == "NO_ID") && (otherId == null || otherId == "" || otherId == "NO_ID")) {
                    continue
                }
                if (thisId != otherId) {
                    return false
                }
            }

            return true
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
