package com.github.grishberg.android.layoutinspector.ui.gropedtables

import java.awt.Component
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableCellRenderer

private const val BORDER = 6

class CustomTableCellRenderer : DefaultTableCellRenderer() {
    override fun getTableCellRendererComponent(
        table: JTable?,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component {
        val component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
        border = BorderFactory.createEmptyBorder(0, BORDER, 0, 0)
        horizontalAlignment = JLabel.LEFT
        return component
    }
}

class TableWithCutOffCellsTooltip : JTable() {
    private val defultCellRenderer = CustomTableCellRenderer()

    override fun getToolTipText(e: MouseEvent): String? {
        var tip: String? = null
        val p: Point = e.getPoint()
        val rowIndex = rowAtPoint(p)
        val colIndex = columnAtPoint(p)

        val bounds: Rectangle = getCellRect(rowIndex, colIndex, false)
        val currentCellRenderer = prepareRenderer(getCellRenderer(rowIndex, colIndex), rowIndex, colIndex)

        if (currentCellRenderer.preferredSize.width > bounds.width) {
            try {
                tip = getValueAt(rowIndex, colIndex).toString()
            } catch (e1: RuntimeException) { //catch null pointer exception if mouse is over an empty line
            }
        }
        return tip
    }

    override fun getCellRenderer(row: Int, column: Int): TableCellRenderer = defultCellRenderer
}