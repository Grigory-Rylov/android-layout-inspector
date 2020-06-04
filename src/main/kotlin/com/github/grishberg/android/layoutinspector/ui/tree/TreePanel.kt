package com.github.grishberg.android.layoutinspector.ui.tree

import com.android.layoutinspector.model.LayoutFileData
import com.android.layoutinspector.model.ViewNode
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.KeyEvent
import javax.swing.JComponent
import javax.swing.JTree
import javax.swing.KeyStroke
import javax.swing.tree.DefaultMutableTreeNode

class TreePanel : JTree(DefaultMutableTreeNode()) {
    var nodeSelectedAction: OnNodeSelectedAction? = null
    private var selectedFromLayoutClick = false
    private val copyTypeStroke =
        KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().menuShortcutKeyMask, false)

    private val copyFullNameStroke = KeyStroke.getKeyStroke(
        KeyEvent.VK_C,
        Toolkit.getDefaultToolkit().menuShortcutKeyMask + ActionEvent.ALT_MASK,
        false
    )
    private val copyIdStroke = KeyStroke.getKeyStroke(
        KeyEvent.VK_C,
        Toolkit.getDefaultToolkit().menuShortcutKeyMask + ActionEvent.SHIFT_MASK,
        false
    )

    private val foundItems = mutableListOf<ViewNode>()
    private val viewNodeRenderer = NodeViewTreeCellRenderer(foundItems)

    init {
        isRootVisible = true
        registerKeyboardAction(CopyShortNameAction(), "Copy", copyTypeStroke, JComponent.WHEN_FOCUSED)
        registerKeyboardAction(CopyFullNameAction(), "Copy", copyFullNameStroke, JComponent.WHEN_FOCUSED)
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

    fun showLayoutResult(resultOutput: LayoutFileData) {
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

    fun highlightFoundItems(items: List<ViewNode>) {
        foundItems.clear()
        foundItems.addAll(items)
        repaint()
    }

    fun removeFoundItemsHighlighting() {
        foundItems.clear()
        repaint()
    }

    private inner class CopyShortNameAction : ActionListener {
        override fun actionPerformed(e: ActionEvent) {
            if (e.actionCommand.compareTo("Copy") != 0) {
                return
            }
            if (selectionPath == null) {
                return
            }

            val selectedValue = selectionPath.lastPathComponent as ViewNode
            copyToClipboard(selectedValue.typeAsString())
        }
    }

    private inner class CopyFullNameAction : ActionListener {
        override fun actionPerformed(e: ActionEvent) {
            if (e.actionCommand.compareTo("Copy") != 0) {
                return
            }
            if (selectionPath == null) {
                return
            }

            val selectedValue = selectionPath.lastPathComponent as ViewNode
            copyToClipboard(selectedValue.name)
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
            copyToClipboard(rawId)
        }
    }

    private fun copyToClipboard(rawId: String) {
        val stringSelection = StringSelection(rawId)
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(stringSelection, null)
    }

    interface OnNodeSelectedAction {
        fun onViewNodeSelected(node: ViewNode)
    }
}
