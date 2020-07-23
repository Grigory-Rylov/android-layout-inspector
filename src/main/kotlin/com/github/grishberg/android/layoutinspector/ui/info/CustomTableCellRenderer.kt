package com.github.grishberg.android.layoutinspector.ui.info

import java.awt.Component
import javax.swing.BorderFactory
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer

class CustomTableCellRenderer : DefaultTableCellRenderer() {
    private val emptyBorder = BorderFactory.createEmptyBorder(0, 0, 0, 8)

    override fun getTableCellRendererComponent(
        table: JTable?,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component {
        val tableCellRendererComponent =
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
        border = emptyBorder
        return tableCellRendererComponent
    }
}
