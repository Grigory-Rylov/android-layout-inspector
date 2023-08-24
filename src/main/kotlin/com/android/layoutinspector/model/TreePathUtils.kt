package com.android.layoutinspector.model

import com.github.grishberg.android.layoutinspector.domain.AbstractViewNode
import com.google.common.collect.Lists
import javax.swing.tree.TreePath

object TreePathUtils {

    /** Finds the path from node to the root.  */
    @JvmStatic
    fun getPath(node: AbstractViewNode): TreePath {
        return getPathImpl(node, null)
    }

    /** Finds the path from node to the parent.  */
    @JvmStatic
    fun getPathFromParent(node: AbstractViewNode, root: AbstractViewNode): TreePath {
        return getPathImpl(node, root)
    }

    private fun getPathImpl(node: AbstractViewNode, root: AbstractViewNode?): TreePath {
        var node: AbstractViewNode? = node
        val nodes = Lists.newArrayList<Any>()
        do {
            nodes.add(0, node)
            node = node?.parent as AbstractViewNode?
        } while (node != null && node !== root)
        if (root != null && node === root) {
            nodes.add(0, root)
        }
        return TreePath(nodes.toTypedArray())
    }
}
