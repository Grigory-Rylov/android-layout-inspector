package com.github.grishberg.gropedtables

import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JPanel


class GroupedTable : JPanel() {
    private val tableContainer = JPanel()

    private val children = mutableListOf<CollapsibleTable>()

    private val expandedGroups = mutableSetOf<String>()
    private val collapsedExpandAction = CollapsedExpandAction()
    val constraints = GridBagConstraints()

    init {
        layout = BorderLayout()
        add(tableContainer, BorderLayout.NORTH)

        // children
        tableContainer.layout = GridBagLayout()

        constraints.anchor = GridBagConstraints.PAGE_START
        constraints.fill = GridBagConstraints.HORIZONTAL
        constraints.weightx = 1.0
        constraints.gridx = 0
    }

    fun updateData(data: GroupedTableDataModel, allExpanded: Boolean = false) {
        tableContainer.removeAll()
        children.clear()
        val groupsCount = data.getGroupsCount()
        for (i in 0 until groupsCount) {
            val groupName = data.getGroupName(i)

            val shouldExpand: Boolean = if (allExpanded) {
                expandedGroups.add(groupName)
                true
            } else {
                expandedGroups.contains(groupName)
            }

            val collapsible = CollapsibleTable(groupName, data.getTableRowInfo(i), !shouldExpand)
            collapsible.collapsedAction = collapsedExpandAction
            addSingleComponent(collapsible)
            children.add(collapsible)
        }
        repaint()
    }

    private fun addSingleComponent(component: JComponent) {
        //add(component, cons)
        tableContainer.add(component, constraints)
    }

    fun expandAll() {
        children.forEach { it.expand() }
    }

    fun collapseAll() {
        children.forEach { it.collapse() }
    }

    private inner class CollapsedExpandAction : CollapsibleTable.CollapseExpandAction {
        override fun onCollapsed(groupName: String) {
            expandedGroups.remove(groupName)
        }

        override fun onExpanded(groupName: String) {
            expandedGroups.add(groupName)
        }
    }
}