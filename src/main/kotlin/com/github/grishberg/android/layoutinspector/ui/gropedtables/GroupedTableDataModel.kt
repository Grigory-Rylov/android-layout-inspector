package com.github.grishberg.android.layoutinspector.ui.gropedtables

interface GroupedTableDataModel {
    fun getGroupsCount(): Int
    fun getTableRowInfo(groupIndex: Int): TableRowInfo
    fun getGroupName(groupIndex: Int): String
}