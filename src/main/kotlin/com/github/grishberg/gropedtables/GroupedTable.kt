package com.github.grishberg.gropedtables

import javax.swing.BoxLayout
import javax.swing.JPanel

class GroupedTable : JPanel() {
    private val children = mutableListOf<Collapsible>()

    private val expandedGroups = mutableSetOf<String>()
    private val collapsedExpandAction = CollapsedExpandAction()

    init {
        layout = BoxLayout(this, BoxLayout.PAGE_AXIS)
    }

    fun updateData(data: GroupedTableDataModel, allExpanded: Boolean = false) {
        removeAll()
        children.clear()
        val groupsCount = data.getGroupsCount()
        for (i in 0 until groupsCount) {
            val groupName = data.getGroupName(i)

            val shouldExpand : Boolean = if (allExpanded) {
                expandedGroups.add(groupName)
                true
            } else {
                expandedGroups.contains(groupName)
            }

            val collapsible = Collapsible(groupName, data.getTableRowInfo(i), !shouldExpand)
            collapsible.collapsedAction = collapsedExpandAction
            add(collapsible)
            children.add(collapsible)
        }
        repaint()
    }

    fun expandAll() {
        children.forEach { it.expand() }
    }

    fun collapseAll() {
        children.forEach { it.collapse() }
    }

    private inner class CollapsedExpandAction: Collapsible.CollapseExpandAction {
        override fun onCollapsed(groupName: String) {
            expandedGroups.remove(groupName)
        }

        override fun onExpanded(groupName: String) {
            expandedGroups.add(groupName)
        }
    }
}