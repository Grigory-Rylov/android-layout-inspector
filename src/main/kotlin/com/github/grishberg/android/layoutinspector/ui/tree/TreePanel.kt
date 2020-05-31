package com.github.grishberg.android.layoutinspector.ui.tree

import com.android.layoutinspector.model.LayoutFileData
import com.android.layoutinspector.model.ViewNode
import java.awt.Color
import java.awt.Component
import javax.swing.ImageIcon
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer

class TreePanel : JTree(DefaultMutableTreeNode()) {
    var nodeSelectedAction: OnNodeSelectedAction? = null
    private var selectedFromLayoutClick = false
    private val hoveredTextColor = Color(45, 71, 180)
    private val hiddenTextColor = Color(0, 0, 0, 127)

    private val viewNodeRenderer = ViewNodeRenderer()

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

    /*
        override fun convertValueToText(
            value: Any?,
            selected: Boolean,
            expanded: Boolean,
            leaf: Boolean,
            row: Int,
            hasFocus: Boolean
        ): String {
            if (value !is ViewNode) {
                return ""
            }

            val typeAsString = typeAsString()
            val text = value.getText()
            if (text != null) {
                return value.getElliptizedText()
            }
            val sValue = value.toString()
            if (sValue != null) {
                return sValue
            }

            return ""
        }
    */
    private fun typeAsString(node: ViewNode): String {
        val lastDotPost = node.name.lastIndexOf(".")
        if (lastDotPost >= 0) {
            return node.name.substring(lastDotPost)
        }
        return node.name
    }

    private inner class ViewNodeRenderer : DefaultTreeCellRenderer() {
        var hoveredNode: ViewNode? = null
        val viewGroupIcon = createImageIcon("icons/rectangle.png")
        val hiddenIcon = createImageIcon("icons/rectangle.png")
        val textIcon = createImageIcon("icons/text.png")

        override fun getTreeCellRendererComponent(
            tree: JTree,
            value: Any,
            sel: Boolean,
            expanded: Boolean,
            leaf: Boolean,
            row: Int,
            hasFocus: Boolean
        ): Component {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus)

            if (value !is ViewNode) {
                return this
            }

            val visible = value.isDrawn
            if (isLayout(value)) {
                icon = viewGroupIcon
            }
            val text = value.getText()
            if (text != null) {
                icon = textIcon
            }
            if (!visible) {
                foreground = hiddenTextColor
            }
            if (value == hoveredNode) {
                foreground = hoveredTextColor
            }
            return this
        }

        private fun isLayout(value: ViewNode): Boolean {
            if (value.childCount > 0) {
                return true
            }
            return false
        }

    }

    interface OnNodeSelectedAction {
        fun onViewNodeSelected(node: ViewNode)
    }
}
