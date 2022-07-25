package com.github.grishberg.android.layoutinspector.ui.info.flat

import com.github.grishberg.android.layoutinspector.ui.info.RowInfoImpl
import javax.swing.table.DefaultTableModel

private const val NAME_COLUMN = 0

class FlatGroupTableModel(
    data: Map<String, List<RowInfoImpl>>
) : DefaultTableModel() {
    private val list = mutableListOf<Row>()

    init {
        prepareData(data)
        columnCount = 2
    }

    private fun prepareData(data: Map<String, List<RowInfoImpl>>) {
        for (e in data.entries) {
            list.add(Row.HeaderRow(e.key))

            for (item in e.value) {
                list.add(Row.ValueRow(item))
            }
        }

        rowCount = list.size
    }

    fun updateData(data: Map<String, List<RowInfoImpl>>) {
        list.clear()
        prepareData(data)
        fireTableDataChanged()
    }

    override fun getColumnName(column: Int): String {
        return when (column) {
            0 -> "Property name"
            else -> "Value"
        }
    }

    override fun getColumnClass(columnIndex: Int): Class<*>? {
        return String::class.java
    }

    override fun isCellEditable(row: Int, column: Int): Boolean = false

    override fun getValueAt(row: Int, column: Int): Any {
        val rowItem = list[row]
        when (rowItem) {

            is Row.ValueRow -> {
                if (column == NAME_COLUMN) {
                    return TableValue.PropertyName(rowItem.property, rowItem.property.isSummary)
                }
                return TableValue.PropertyValue(rowItem.property)
            }
            is Row.HeaderRow -> {
                if (column == NAME_COLUMN) {
                    return TableValue.Header(rowItem.name)
                }
                return TableValue.Empty
            }
        }
    }
}