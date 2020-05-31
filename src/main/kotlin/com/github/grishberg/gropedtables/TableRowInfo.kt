package com.github.grishberg.gropedtables

interface TableRowInfo {
    fun getColumnName(col: Int): String
    fun getColumnCount(): Int
    fun getRowCount(): Int
    fun getValueAt(row: Int, col: Int) : String
}