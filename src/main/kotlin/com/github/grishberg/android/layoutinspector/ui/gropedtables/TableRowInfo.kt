package com.github.grishberg.android.layoutinspector.ui.gropedtables

interface TableRowInfo {
    fun getColumnName(col: Int): String
    fun getColumnCount(): Int
    fun getRowCount(): Int
    fun getValueAt(row: Int, col: Int) : String
}