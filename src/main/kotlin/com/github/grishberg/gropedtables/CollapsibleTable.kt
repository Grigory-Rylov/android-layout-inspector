package com.github.grishberg.gropedtables

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.table.AbstractTableModel


class CollapsibleTable(
    private val title: String, tableData: TableRowInfo, collapsed: Boolean
) : JPanel() {
    private val table = TableWithCutOffCellsTooltip()
    private val collapseBtn: JButton
    private val tableData: TableRowInfo
    private var isCollapsed: Boolean

    var collapsedAction: CollapseExpandAction? = null

    init {
        val copyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().menuShortcutKeyMask, false)
        table.registerKeyboardAction(CopyAction(), "Copy", copyStroke, JComponent.WHEN_FOCUSED)
        collapseBtn = JButton(title)
        this.tableData = tableData
        layout = BorderLayout()
        table.setGridColor(Color.GRAY)
        table.fillsViewportHeight = true
        table.model = TableModel()
        isCollapsed = collapsed
        table.isVisible = !collapsed
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)

        add(table, BorderLayout.CENTER)
        collapseBtn.addActionListener {
            if (!isCollapsed) {
                collapse()
            } else {
                expand()
            }
        }
        add(collapseBtn, BorderLayout.NORTH)
    }

    fun collapse() {
        table.isVisible = false
        isCollapsed = true
        collapsedAction?.onCollapsed(title)
    }

    fun expand() {
        table.isVisible = true
        isCollapsed = false
        collapsedAction?.onExpanded(title)
    }

    private inner class TableModel : AbstractTableModel() {
        override fun getColumnName(col: Int): String {
            return tableData.getColumnName(col)
        }

        override fun getRowCount(): Int {
            return tableData.getRowCount()
        }

        override fun getColumnCount(): Int {
            return tableData.getColumnCount()
        }

        override fun getValueAt(row: Int, col: Int): Any {
            return tableData.getValueAt(row, col)
        }

        override fun isCellEditable(row: Int, col: Int): Boolean {
            return false
        }
    }

    private inner class CopyAction : ActionListener {
        override fun actionPerformed(e: ActionEvent) {
            if (e.getActionCommand().compareTo("Copy") == 0) {
                val sbf = StringBuffer()
                val numcols: Int = table.columnModel.columnCount
                val numrows: Int = table.getSelectedRowCount()

                if (numrows < 1) {
                    JOptionPane.showMessageDialog(
                        null, "Invalid Copy Selection",
                        "Invalid Copy Selection", JOptionPane.ERROR_MESSAGE
                    )
                    return
                }
                val rowsselected = table.getSelectedRows().first()

                for (j in 0 until numcols) {
                    sbf.append(table.getValueAt(rowsselected, j))
                    if (j < numcols - 1) sbf.append("\t")
                }

                val stringSelection = StringSelection(sbf.toString())
                val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                clipboard.setContents(stringSelection, null)
            }
        }
    }

    interface CollapseExpandAction {
        fun onCollapsed(groupName: String)
        fun onExpanded(groupName: String)
    }


}