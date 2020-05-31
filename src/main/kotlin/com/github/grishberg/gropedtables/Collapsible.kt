package com.github.grishberg.gropedtables

import java.awt.BorderLayout
import java.awt.Color
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.table.AbstractTableModel

class Collapsible(
    private val title: String, tableData: TableRowInfo, collapsed: Boolean
) : JPanel() {
    private val table = JTable()
    private val collapseBtn: JButton
    private val tableData: TableRowInfo
    private var isCollapsed: Boolean

    var collapsedAction: CollapseExpandAction? = null

    init {
        collapseBtn = JButton(title)
        this.tableData = tableData
        layout = BorderLayout()
        table.setGridColor(Color.GRAY)
        table.fillsViewportHeight = true
        table.model = TableModel()
        isCollapsed = collapsed
        table.isVisible = !collapsed
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

    interface CollapseExpandAction {
        fun onCollapsed(groupName: String)
        fun onExpanded(groupName: String)
    }


}