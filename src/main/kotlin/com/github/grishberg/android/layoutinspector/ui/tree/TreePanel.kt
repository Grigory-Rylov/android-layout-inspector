package com.github.grishberg.android.layoutinspector.ui.tree

import com.android.layoutinspector.model.LayoutFileData
import com.android.layoutinspector.model.ViewNode
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.KeyEvent
import javax.swing.ImageIcon
import javax.swing.JComponent
import javax.swing.JTree
import javax.swing.KeyStroke
import javax.swing.tree.DefaultMutableTreeNode

class TreePanel : JTree(DefaultMutableTreeNode()) {
    var nodeSelectedAction: OnNodeSelectedAction? = null
    private var selectedFromLayoutClick = false
    private val copyTypeStroke =
        KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().menuShortcutKeyMask, false)
    private val copyIdStroke = KeyStroke.getKeyStroke(
        KeyEvent.VK_C,
        Toolkit.getDefaultToolkit().menuShortcutKeyMask + ActionEvent.SHIFT_MASK,
        false
    )

    private val viewNodeRenderer = NodeViewTreeCellRenderer()

    init {
        isRootVisible = true
        registerKeyboardAction(CopyTypeAction(), "Copy", copyTypeStroke, JComponent.WHEN_FOCUSED)
        registerKeyboardAction(CopyIdAction(), "Copy", copyIdStroke, JComponent.WHEN_FOCUSED)

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

    private inner class CopyTypeAction : ActionListener {
        override fun actionPerformed(e: ActionEvent) {
            if (e.actionCommand.compareTo("Copy") != 0) {
                return
            }
            if (selectionPath == null) {
                return
            }

            val selectedValue = selectionPath.lastPathComponent as ViewNode
            val stringSelection = StringSelection(selectedValue.typeAsString())
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            clipboard.setContents(stringSelection, null)
        }
    }

    private inner class CopyIdAction : ActionListener {
        override fun actionPerformed(e: ActionEvent) {
            if (e.actionCommand.compareTo("Copy") != 0) {
                return
            }
            if (selectionPath == null) {
                return
            }

            val selectedValue = selectionPath.lastPathComponent as ViewNode
            val id: String = selectedValue.id ?: return

            val rawId = if (id.startsWith("id/")) {
                id.substring(3)
            } else {
                id
            }
            val stringSelection = StringSelection(rawId)
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            clipboard.setContents(stringSelection, null)
        }
    }

    interface OnNodeSelectedAction {
        fun onViewNodeSelected(node: ViewNode)
    }
}
