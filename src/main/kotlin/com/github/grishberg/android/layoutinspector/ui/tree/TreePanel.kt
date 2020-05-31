package com.github.grishberg.android.layoutinspector.ui.tree

import com.android.layoutinspector.model.LayoutFileData
import com.android.layoutinspector.model.ViewNode
import javax.swing.ImageIcon
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode

class TreePanel : JTree(DefaultMutableTreeNode()) {
    var nodeSelectedAction: OnNodeSelectedAction? = null
    private var selectedFromLayoutClick = false

    private val viewNodeRenderer = NodeViewTreeCellRenderer()

    init {
        isRootVisible = true

        addTreeSelectionListener {
            if (selectedFromLayoutClick) {
                selectedFromLayoutClick = false
                return@addTreeSelectionListener
            }
            val path = it.path
            val selectedNode = path.lastPathComponent
            if (selectedNode is ViewNode) {
                if (hasFocus()) {
                    nodeSelectedAction?.onViewNodeSelected(selectedNode)
                }
            }
        }

        setCellRenderer(viewNodeRenderer)
    }

    /** Returns an ImageIcon, or null if the path was invalid.  */
    private fun createImageIcon(path: String): ImageIcon? {
        val imgURL = ClassLoader.getSystemResource(path)
        return if (imgURL != null) {
            ImageIcon(imgURL)
        } else {
            System.err.println("Couldn't find file: $path")
            null
        }
    }


    fun showLayoutResult(resultOutput: LayoutFileData) {
        if (resultOutput.node == null) {
            return
        }

        resultOutput.node?.let {
            model = ViewNodeTreeModel(it)
            expandAllNodes(this)
        }
    }

    private fun expandAllNodes(tree: JTree) {
        var j = tree.rowCount
        var i = 0
        while (i < j) {
            tree.expandRow(i)
            i += 1
            j = tree.rowCount
        }
    }

    fun onNodeHovered(node: ViewNode) {
        viewNodeRenderer.hoveredNode = node
        repaint()
    }

    fun onNodeSelected(node: ViewNode) {
        selectedFromLayoutClick = true
        val path = ViewNode.getPath(node)
        selectionPath = path
        scrollPathToVisible(path)
    }

    fun removeHovered() {
        viewNodeRenderer.hoveredNode = null
        repaint()
    }

    interface OnNodeSelectedAction {
        fun onViewNodeSelected(node: ViewNode)
    }
}
