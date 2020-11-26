package com.github.grishberg.android.layoutinspector.ui.info

import com.android.layoutinspector.model.ViewNode
import com.android.layoutinspector.model.ViewProperty
import com.github.grishberg.android.layoutinspector.domain.MetaRepository
import com.github.grishberg.expandabletree.JTreeTable
import com.github.grishberg.expandabletree.model.GroupedTableModel
import com.github.grishberg.expandabletree.model.RowInfo
import com.github.grishberg.expandabletree.model.TableRowInfo
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.KeyEvent
import java.math.RoundingMode
import java.text.DecimalFormat
import javax.swing.*
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeExpansionListener


/**
 * Shows nodes info.
 */
class PropertiesPanel(
    private val meta: MetaRepository
) {
    private var currentNode: ViewNode? = null
    private val table = JTreeTable(GroupedTableModel(emptyList()))
    private val scrollPanel = JScrollPane(table)
    private var sizeInDp = false
    private val expandedGroups = mutableSetOf<String>()

    init {
        table.showVerticalLines = true
        table.showHorizontalLines = true
        val tableRenderer = table.treeTableCellRenderer
        tableRenderer.isRootVisible = false

        val copyStroke = KeyStroke.getKeyStroke(
            KeyEvent.VK_C,
            Toolkit.getDefaultToolkit().menuShortcutKeyMask,
            false
        )
        table.registerKeyboardAction(CopyAction(), "Copy", copyStroke, JComponent.WHEN_FOCUSED)
        table.addTreeExpansionListener(object : TreeExpansionListener {
            override fun treeExpanded(event: TreeExpansionEvent) {
                val expandexGroup = event.path.lastPathComponent as TableRowInfoImpl
                expandedGroups.add(expandexGroup.toString())
            }

            override fun treeCollapsed(event: TreeExpansionEvent) {
                val expandexGroup = event.path.lastPathComponent as TableRowInfoImpl
                expandedGroups.remove(expandexGroup.toString())
            }
        })

        val selectionModel = table.selectionModel
        selectionModel.addListSelectionListener { e -> table.repaint() }
    }

    fun getComponent(): JComponent = scrollPanel

    fun showProperties(node: ViewNode) {
        currentNode = node
        val createPropertiesData = createPropertiesData(node)
        table.setModel(GroupedTableModel(createPropertiesData))

        val rightRenderer = CustomTableCellRenderer()
        rightRenderer.horizontalAlignment = JLabel.RIGHT
        table.columnModel.getColumn(1).cellRenderer = rightRenderer

        val treeFont = table.treeTableCellRenderer.font
        table.tree.rowHeight = treeFont.size + 6

        for (expanded in expandedGroups) {
            val rowIndexByName = rowIndexByName(createPropertiesData, expanded)
            if (rowIndexByName >= 0) {
                table.tree.expandRow(rowIndexByName)
            }
        }
    }

    private fun rowIndexByName(createPropertiesData: List<TableRowInfo>, expanded: String): Int {
        for (i in createPropertiesData.indices) {
            if (createPropertiesData[i].toString() == expanded) {
                return i
            }
        }
        return -1
    }

    private fun createPropertiesData(node: ViewNode): List<TableRowInfo> {
        val groups = mutableListOf<TableRowInfoImpl>()
        for (entry in node.groupedProperties) {
            groups.add(TableRowInfoImpl(entry.key, entry.value, sizeInDp, meta.dpPerPixels))
        }
        return groups
    }

    fun setSizeDpMode(enabled: Boolean) {
        val shouldInvalidate = sizeInDp != enabled
        sizeInDp = enabled
        if (shouldInvalidate) {
            currentNode?.let {
                table.setModel(GroupedTableModel(createPropertiesData(it)))
            }
        }
    }

    /**
     * Table model.
     */
    private class TableRowInfoImpl(
        private val name: String,
        properties: List<ViewProperty>,
        sizeInDp: Boolean,
        dpPerPixels: Double
    ) : TableRowInfo {
        private val rows = mutableListOf<RowInfo>()

        init {
            for (property in properties) {
                rows.add(RowInfoImpl(property, sizeInDp, dpPerPixels))
            }
        }

        override fun getRowCount(): Int {
            return rows.size
        }

        override fun getRowAt(row: Int): RowInfo {
            return rows[row]
        }

        override fun toString(): String {
            return name
        }
    }

    /**
     * Table row value model.
     */
    private class RowInfoImpl(
        private val property: ViewProperty,
        private val sizeInDp: Boolean,
        private val dpPerPixels: Double
    ) : RowInfo {
        override fun value(): String {

            if (sizeInDp && property.isSizeProperty && dpPerPixels > 1) {
                return roundOffDecimal(property.intValue.toDouble() / dpPerPixels) + " dp"
            }
            return property.value
        }

        override fun toString(): String {
            return property.name
        }

        fun roundOffDecimal(number: Double): String {
            val df = DecimalFormat("#.##")
            df.roundingMode = RoundingMode.CEILING
            return df.format(number)
        }
    }

    private inner class CopyAction : ActionListener {
        override fun actionPerformed(e: ActionEvent) {
            if (e.actionCommand.compareTo("Copy") != 0) {
                return
            }
            val sbf = StringBuffer()
            val numcols: Int = table.columnModel.columnCount
            val numrows: Int = table.selectedRowCount

            if (numrows < 1) {
                JOptionPane.showMessageDialog(
                    null, "Invalid Copy Selection",
                    "Invalid Copy Selection", JOptionPane.ERROR_MESSAGE
                )
                return
            }
            val rowsselected = table.selectedRows.first()

            for (j in 0 until numcols) {
                val valueAt = table.getValueAt(rowsselected, j)
                if (valueAt != null) {
                    sbf.append(valueAt)
                }
                if (j < numcols - 1) sbf.append("\t")
            }

            val stringSelection = StringSelection(sbf.toString())
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            clipboard.setContents(stringSelection, null)
        }
    }

}

