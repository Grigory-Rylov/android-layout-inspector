package com.github.grishberg.gropedtables

interface GroupedTableDataModel {
    fun getGroupsCount(): Int
    fun getTableRowInfo(groupIndex: Int): TableRowInfo
    fun getGroupName(groupIndex: Int): String
}