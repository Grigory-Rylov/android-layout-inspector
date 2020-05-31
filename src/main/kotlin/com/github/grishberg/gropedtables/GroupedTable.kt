package com.github.grishberg.gropedtables

import com.github.grishberg.android.layoutinspector.ui.common.LabeledGridBuilder
import java.awt.Color
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JComponent


class GroupedTable : Box(BoxLayout.Y_AXIS) {
    private val children = mutableListOf<Collapsible>()

    private val expandedGroups = mutableSetOf<String>()
    private val collapsedExpandAction = CollapsedExpandAction()

    init {
        background = Color.CYAN
        //border = EmptyBorder(8, 8, 8, 8)
    }

    fun updateData(data: GroupedTableDataModel, allExpanded: Boolean = false) {
        val builder = LabeledGridBuilder()
        removeAll()
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

            val collapsible = Collapsible(groupName, data.getTableRowInfo(i), !shouldExpand)
            collapsible.collapsedAction = collapsedExpandAction
            addSingleComponent(collapsible)
            children.add(collapsible)
        }
        add(builder.content)
        repaint()
    }

    private fun addSingleComponent(component: JComponent) {
        add(component)
    }

    fun expandAll() {
        children.forEach { it.expand() }
    }

    fun collapseAll() {
        children.forEach { it.collapse() }
    }

    private inner class CollapsedExpandAction : Collapsible.CollapseExpandAction {
        override fun onCollapsed(groupName: String) {
            expandedGroups.remove(groupName)
        }

        override fun onExpanded(groupName: String) {
            expandedGroups.add(groupName)
        }
    }
}