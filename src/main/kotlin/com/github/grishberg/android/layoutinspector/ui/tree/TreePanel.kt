package com.github.grishberg.android.layoutinspector.ui.tree

import com.android.layoutinspector.model.LayoutFileData
import com.android.layoutinspector.model.ViewNode
import com.github.grishberg.android.layoutinspector.domain.MetaRepository
import com.github.grishberg.android.layoutinspector.ui.Main
import com.github.grishberg.android.layoutinspector.ui.dialogs.bookmarks.Bookmarks
import com.github.grishberg.android.layoutinspector.ui.theme.ThemeColors
import java.awt.Point
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.JTree
import javax.swing.KeyStroke
import javax.swing.SwingUtilities
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeCellRenderer
import javax.swing.tree.TreeSelectionModel


class TreePanel(
    private val frame: JFrame,
    private val theme: ThemeColors,
    private val meta: MetaRepository,
    private val bookmarks: Bookmarks,
    private val main: Main
) : JTree(DefaultMutableTreeNode()) {
    var nodeSelectedAction: OnNodeSelectedAction? = null
    private var selectedFromLayoutClick = false
    private val copyTypeStroke = KeyStroke.getKeyStroke(
        KeyEvent.VK_C,
        Toolkit.getDefaultToolkit().menuShortcutKeyMask + ActionEvent.ALT_MASK,
        false
    )

    private val copyFullNameStroke = KeyStroke.getKeyStroke(
        KeyEvent.VK_C,
        Toolkit.getDefaultToolkit().menuShortcutKeyMask + ActionEvent.SHIFT_MASK,
        false
    )
    private val copyIdStroke =
        KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().menuShortcutKeyMask, false)

    private val foundItems = mutableListOf<ViewNode>()
    private var viewNodeRenderer = NodeViewTreeCellRenderer(foundItems, theme, bookmarks)

    init {
        isRootVisible = true
        registerKeyboardAction(CopyShortNameAction(), "Copy", copyTypeStroke, JComponent.WHEN_FOCUSED)
        registerKeyboardAction(CopyFullNameAction(), "Copy", copyFullNameStroke, JComponent.WHEN_FOCUSED)
        registerKeyboardAction(CopyIdAction(), "Copy", copyIdStroke, JComponent.WHEN_FOCUSED)

        addTreeSelectionListener {
            var hasSelections = false
            for (i in it.paths.indices) {
                if (it.isAddedPath(i)) {
                    hasSelections = true
                    break
                }
            }

            if (!hasSelections) {
                return@addTreeSelectionListener
            }

            if (selectedFromLayoutClick) {
                selectedFromLayoutClick = false
                return@addTreeSelectionListener
            }
            val path = it.path
            val selectedNode = path.lastPathComponent
            if (selectedNode is ViewNode) {
                nodeSelectedAction?.onViewNodeSelected(selectedNode)
                repaint()
            }
        }
        addMouseMotionListener(object : MouseAdapter() {
            override fun mouseMoved(e: MouseEvent) {
                val nodeByPoint = nodeByPoint(e.point)
                if (nodeByPoint != null) {
                    nodeSelectedAction?.onViewNodeHovered(nodeByPoint)
                } else {
                    nodeSelectedAction?.onViewNodeNotHovered()
                }
            }
        })
        addMouseListener(object : MouseListener {
            override fun mouseReleased(e: MouseEvent) = Unit

            override fun mouseEntered(e: MouseEvent) = Unit

            override fun mouseClicked(e: MouseEvent) = Unit

            override fun mouseExited(e: MouseEvent) = Unit

            override fun mousePressed(e: MouseEvent) {
                if (!SwingUtilities.isRightMouseButton(e)) {
                    return
                }
                val nodeByPoint = nodeByPoint(e.point) ?: return
                showContextMenu(nodeByPoint, e.point)
            }
        })
        setCellRenderer(viewNodeRenderer)

        getSelectionModel().selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION

        theme.addColorChangedAction {
            viewNodeRenderer = NodeViewTreeCellRenderer(foundItems, theme, bookmarks)
            setCellRenderer(viewNodeRenderer)
        }
        bookmarks.listeners.add {
            repaint()
        }
    }

    private fun calculateDistance(targetNode: ViewNode) {
        if (selectionPath == null) {
            return
        }

        val selectedValue = selectionPath.lastPathComponent as ViewNode

        main.calculateDistance(selectedValue, targetNode)
    }

    private fun showContextMenu(nodeByPoint: ViewNode, point: Point) {
        val calculateDistanceDelegate: CalculateDistanceDelegate? = if (selectionPath == null) null
        else {
            { calculateDistance(nodeByPoint) }
        }
        val popupMenu = TreeViewNodeMenu(frame, this, nodeByPoint, meta, bookmarks, calculateDistanceDelegate)
        popupMenu.show(this, point.x, point.y)
    }

    override fun getCellRenderer(): TreeCellRenderer? {
        if (viewNodeRenderer == null) {
            return super.getCellRenderer()
        }
        return viewNodeRenderer
    }

    private fun nodeByPoint(point: Point): ViewNode? {
        val selRow = getRowForLocation(point.x, point.y)
        val r = getCellRenderer()
        if (selRow != -1 && r != null) {
            val path = getPathForRow(selRow)
            val selectedNode = path.lastPathComponent
            if (selectedNode is ViewNode) {
                return selectedNode
            }
        }
        return null
    }

    fun showLayoutResult(resultOutput: LayoutFileData) {
        resultOutput.node?.let {
            model = ViewNodeTreeModel(it)
            expandAllNodes(this)
            invalidate()
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

    fun copyShortNameToClipboard() {
        val selectedValue = selectionPath.lastPathComponent as ViewNode
        copyToClipboard(selectedValue.typeAsString())
    }

    fun copyIdToClipboard() {
        val selectedValue = selectionPath.lastPathComponent as ViewNode
        val id: String = selectedValue.id ?: return

        val rawId = if (id.startsWith("id/")) {
            id.substring(3)
        } else {
            id
        }
        copyToClipboard(rawId)
    }

    private inner class CopyShortNameAction : ActionListener {
        override fun actionPerformed(e: ActionEvent) {
            if (e.actionCommand.compareTo("Copy") != 0) {
                return
            }
            if (selectionPath == null) {
                return
            }

            copyShortNameToClipboard()
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

            copyIdToClipboard()
        }
    }

    private fun copyToClipboard(rawId: String) {
        val stringSelection = StringSelection(rawId)
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(stringSelection, null)
    }

    interface OnNodeSelectedAction {
        fun onViewNodeSelected(node: ViewNode)
        fun onViewNodeHovered(node: ViewNode)
        fun onViewNodeNotHovered()
    }
}
