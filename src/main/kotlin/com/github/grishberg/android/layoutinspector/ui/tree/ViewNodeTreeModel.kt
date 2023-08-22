package com.github.grishberg.android.layoutinspector.ui.tree

import com.github.grishberg.android.layoutinspector.domain.AbstractViewNode
import javax.swing.event.TreeModelListener
import javax.swing.tree.TreeModel
import javax.swing.tree.TreePath

class ViewNodeTreeModel(private val viewNode: AbstractViewNode) : TreeModel {
    override fun getRoot() = viewNode

    override fun isLeaf(node: Any?): Boolean {
        if (node is AbstractViewNode) {
            return node.isLeaf
        }
        return false
    }

    override fun getChildCount(parent: Any?): Int {
        if (parent is AbstractViewNode) {
            return parent.childCount
        }
        return 0
    }

    override fun removeTreeModelListener(l: TreeModelListener?) {
    }

    override fun valueForPathChanged(path: TreePath?, newValue: Any?) {
    }

    override fun getIndexOfChild(parent: Any?, child: Any?): Int {
        if (parent is AbstractViewNode && child is AbstractViewNode) {
            return parent.getIndex(child)
        }
        return -1
    }

    override fun getChild(parent: Any?, index: Int): Any {
        if (parent is AbstractViewNode) {
            return parent.getChildAt(index)
        }
        throw IllegalStateException("No child at $index from $parent")
    }

    override fun addTreeModelListener(l: TreeModelListener?) {
    }
}
