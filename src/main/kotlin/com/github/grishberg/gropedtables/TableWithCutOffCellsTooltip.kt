package com.github.grishberg.gropedtables

import java.awt.Point
import java.awt.Rectangle
import java.awt.event.MouseEvent
import javax.swing.JTable


class TableWithCutOffCellsTooltip : JTable() {
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
}