package com.github.grishberg.android.layoutinspector.ui.info.flat

import com.github.grishberg.android.layoutinspector.ui.theme.ThemeColors
import java.awt.Component
import java.awt.Insets
import javax.swing.JTable
import javax.swing.border.CompoundBorder
import javax.swing.border.EmptyBorder
import javax.swing.table.DefaultTableCellRenderer


private const val BORDER_SIZE = 4

class BoardTableCellRenderer(
    private val themeColors: ThemeColors
) : DefaultTableCellRenderer() {

    private var itemHeight = 0

    override fun getTableCellRendererComponent(
        table: JTable, value: Any,
        isSelected: Boolean, hasFocus: Boolean, row: Int, col: Int
    ): Component {
        val c = super.getTableCellRendererComponent(
            table, value,
            isSelected, hasFocus, row, col
        )
        if (itemHeight == 0) {
            itemHeight = ui.getPreferredSize(this)?.height ?: 36
        }

        val viewRow = table.convertRowIndexToModel(row)
        if (viewRow < 0) {
            return c
        }
        val valueAt = table.model.getValueAt(viewRow, col)

        when (valueAt) {
            is TableValue.Empty,
            is TableValue.Header -> {
                border = CompoundBorder(
                    EmptyBorder(Insets(BORDER_SIZE, BORDER_SIZE, BORDER_SIZE, BORDER_SIZE)),
                    border
                )
                c.foreground = themeColors.groupForeground
                c.background = themeColors.groupBackground
                table.setRowHeight(row, itemHeight + BORDER_SIZE * 2)
            }
            is TableValue.PropertyValue,
            is TableValue.PropertyName -> {
                if (isSelected) {
                    return c
                }
                c.foreground = table.foreground
                c.background = themeColors.propertiesPanelBackground
            }
        }

        return c
    }
}