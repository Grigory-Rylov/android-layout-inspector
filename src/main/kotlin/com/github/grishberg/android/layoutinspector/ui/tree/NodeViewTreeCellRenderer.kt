package com.github.grishberg.android.layoutinspector.ui.tree

import com.android.layoutinspector.model.ViewNode
import java.awt.Color
import java.awt.Component
import javax.swing.ImageIcon
import javax.swing.JTree
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.TreeCellRenderer

class NodeViewTreeCellRenderer : TreeCellRenderer {
    var hoveredNode: ViewNode? = null

    val viewGroupIcon = createImageIcon("icons/rectangle.png")

    private val hoveredTextColor = Color(45, 71, 180)
    private val hiddenTextColor = Color(0, 0, 0, 127)

    private val textViewRenderer = TextViewRenderer()
    private val defaultCellRenderer = DefaultTreeCellRenderer()

    override fun getTreeCellRendererComponent(
        tree: JTree?,
        value: Any?,
        selected: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean
    ): Component {
        if (value !is ViewNode) {
            return defaultCellRenderer.getTreeCellRendererComponent(
                tree,
                value,
                selected,
                expanded,
                leaf,
                row,
                hasFocus
            )
        }

        val visible = value.isDrawn
        if (isLayout(value)) {
            defaultCellRenderer.icon = viewGroupIcon
        }

        val text = value.getText()
        if (text != null) {
            textViewRenderer.setNameAndText(value.typeAsString(), value.getElliptizedText(text))
            textViewRenderer.setSelected(selected)
            return textViewRenderer
        }

        if (!visible) {
            defaultCellRenderer.foreground = hiddenTextColor
        }
        if (value == hoveredNode) {
            defaultCellRenderer.foreground = hoveredTextColor
        }
        return defaultCellRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus)
    }

    private fun isLayout(value: ViewNode): Boolean {
        if (value.childCount > 0) {
            return true
        }
        return false
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
}