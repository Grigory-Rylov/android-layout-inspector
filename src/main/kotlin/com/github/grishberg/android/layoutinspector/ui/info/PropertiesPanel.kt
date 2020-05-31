package com.github.grishberg.android.layoutinspector.ui.info

import com.android.layoutinspector.model.ViewNode
import com.android.layoutinspector.model.ViewProperty
import com.github.grishberg.gropedtables.GroupedTable
import com.github.grishberg.gropedtables.GroupedTableDataModel
import com.github.grishberg.gropedtables.TableRowInfo
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JScrollPane


/**
 * Shows nodes info.
 */
class PropertiesPanel {
    private val treeTable = GroupedTable().apply {
        //minimumSize = Dimension(200,480)
    }
    private val scrollPanel = JScrollPane(treeTable)

    fun getComponent(): JComponent = scrollPanel

    fun showProperties(node: ViewNode) {
        treeTable.updateData(TreeTableModel(node))
        scrollPanel.repaint()
        treeTable.repaint()
    }

    private class TreeTableModel(
        private val node: ViewNode
    ) : GroupedTableDataModel {

        private val headers = mutableListOf<String>().apply {
            for (entry in node.groupedProperties) {
                add(entry.key)
            }
        }

        override fun getGroupsCount(): Int = node.groupedProperties.size

        override fun getTableRowInfo(groupIndex: Int): TableRowInfo {
            val key = headers[groupIndex]
            return ParameterModel(node.groupedProperties.getValue(key))
        }

        override fun getGroupName(groupIndex: Int): String = headers[groupIndex]
    }

    private class ParameterModel(
        private val properties: List<ViewProperty>
    ) : TableRowInfo {
        override fun getColumnName(col: Int): String {
            if (col == 0) {
                return "name"
            }
            return "value"
        }

        override fun getColumnCount() = 2

        override fun getRowCount() = properties.size

        override fun getValueAt(row: Int, col: Int): String {
            val currentProperty = properties[row]

            if (col == 0) {
                return currentProperty.name
            }
            return currentProperty.value
        }
    }
}
