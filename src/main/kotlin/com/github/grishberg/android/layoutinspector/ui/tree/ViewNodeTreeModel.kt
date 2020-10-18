package com.github.grishberg.android.layoutinspector.ui.tree

import com.android.layoutinspector.model.ViewNode
import javax.swing.event.TreeModelListener
import javax.swing.tree.TreeModel
import javax.swing.tree.TreePath

class ViewNodeTreeModel(private val viewNode: ViewNode) : TreeModel {
    override fun getRoot() = viewNode

    override fun isLeaf(node: Any?): Boolean {
        if (node is ViewNode) {
            return node.isLeaf
        }
        return false
    }

    override fun getChildCount(parent: Any?): Int {
        if (parent is ViewNode) {
            return parent.childCount
        }
        return 0
    }

    override fun removeTreeModelListener(l: TreeModelListener?) {
    }

    override fun valueForPathChanged(path: TreePath?, newValue: Any?) {
    }

    override fun getIndexOfChild(parent: Any?, child: Any?): Int {
        if (parent is ViewNode && child is ViewNode) {
            return parent.getIndex(child)
        }
        return -1
    }

    override fun getChild(parent: Any?, index: Int): Any {
        if (parent is ViewNode) {
            return parent.getChildAt(index)
        }
        throw IllegalStateException("No child at $index from $parent")
    }

    override fun addTreeModelListener(l: TreeModelListener?) {
    }
}
