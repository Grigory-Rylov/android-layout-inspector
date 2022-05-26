package com.github.grishberg.android.layoutinspector.ui.info.flat

import com.android.layoutinspector.model.ViewNode
import com.github.grishberg.android.layoutinspector.domain.MetaRepository
import com.github.grishberg.android.layoutinspector.ui.info.PropertiesPanel
import com.github.grishberg.android.layoutinspector.ui.info.RowInfoImpl
import com.github.grishberg.android.layoutinspector.ui.theme.ThemeColors
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.KeyEvent
import java.util.regex.PatternSyntaxException
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.JTextField
import javax.swing.KeyStroke
import javax.swing.ListSelectionModel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.table.TableRowSorter

class FlatPropertiesWithFilterPanel(
    private val meta: MetaRepository,
    themeColors: ThemeColors,
) : JPanel(), PropertiesPanel {
    private var currentNode: ViewNode? = null
    private var sorter: TableRowSorter<FlatGroupTableModel>
    private val table: JTable
    private var sizeInDp = false
    private val model = FlatGroupTableModel(emptyMap())

    init {
        layout = BorderLayout()

        table = JTable(model)
        table.autoResizeMode = JTable.AUTO_RESIZE_LAST_COLUMN

        sorter = TableRowSorter(model)
        table.rowSorter = sorter

        val scrollPane = JScrollPane(table)
        //scrollPane.setBounds(5, 10, 300, 150)
// Force the scrollbars to always be displayed
        scrollPane.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS
        )
        scrollPane.setVerticalScrollBarPolicy(
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS
        )

        table.setDefaultRenderer(String::class.java, BoardTableCellRenderer(themeColors))
        table.isFocusable = true
        table.setShowGrid(true)
        table.showHorizontalLines = true
        table.rowMargin = 0
        table.intercellSpacing = Dimension(0, 2)
        table.rowSelectionAllowed = true
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)

        table.isVisible = true

        val copyStroke = KeyStroke.getKeyStroke(
            KeyEvent.VK_C,
            Toolkit.getDefaultToolkit().menuShortcutKeyMask,
            false
        )
        table.registerKeyboardAction(CopyAction(), "Copy", copyStroke, JComponent.WHEN_FOCUSED)

        add(scrollPane, BorderLayout.CENTER)
        add(createFilterPanel(), BorderLayout.NORTH)


        this.preferredSize = Dimension(
            table.preferredSize.width,
            table.preferredSize.height + 85
        )
    }

    private fun createFilterPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        val label = JLabel("Filter")
        panel.add(label, BorderLayout.WEST);
        val filterText = JTextField("")
        panel.add(filterText, BorderLayout.CENTER)

        filterText.document.addDocumentListener(object : DocumentListener {
            override fun changedUpdate(e: DocumentEvent) {
                onTextChanged()
            }

            override fun removeUpdate(e: DocumentEvent) {
                onTextChanged()
            }

            override fun insertUpdate(e: DocumentEvent) {
                onTextChanged()
            }

            private fun onTextChanged() {
                filter(filterText.text)
            }
        })
        return panel
    }

    private fun filter(text: String) {
        if (text.isNotEmpty()) {
            try {
                val filter = PropertiesTableFilter(text)
                sorter.setRowFilter(filter)
            } catch (pse: PatternSyntaxException) {
                println("Bad regex pattern")
            }
        } else {
            sorter.rowFilter = null
        }
    }

    override fun getComponent(): JComponent = this

    override fun showProperties(node: ViewNode) {
        currentNode = node
        val createPropertiesData = createPropertiesData(node)
        model.updateData(createPropertiesData)
        table.repaint()
    }

    private fun createPropertiesData(node: ViewNode): Map<String, List<RowInfoImpl>> {
        val result = mutableMapOf<String, List<RowInfoImpl>>()
        for (entry in node.groupedProperties) {

            val rows = mutableListOf<RowInfoImpl>()
            for (property in entry.value) {
                rows.add(RowInfoImpl(property, sizeInDp, meta.dpPerPixels))
            }
            result[entry.key] = rows
        }
        return result
    }

    override fun setSizeDpMode(enabled: Boolean) {
        val shouldInvalidate = sizeInDp != enabled
        sizeInDp = enabled
        if (shouldInvalidate) {
            currentNode?.let {
                table.model = FlatGroupTableModel(createPropertiesData(it))
            }
        }
    }

    private inner class CopyAction : ActionListener {
        override fun actionPerformed(e: ActionEvent) {
            if (e.actionCommand.compareTo("Copy") != 0) {
                return
            }
            val sbf = StringBuffer()
            val numcols: Int = table.columnModel.columnCount
            val numrows: Int = table.selectedRowCount

            if (numrows < 1) {
                JOptionPane.showMessageDialog(
                    null, "Invalid Copy Selection",
                    "Invalid Copy Selection", JOptionPane.ERROR_MESSAGE
                )
                return
            }
            val rowsselected = table.selectedRows.first()

            for (j in 0 until numcols) {
                val valueAt = table.getValueAt(rowsselected, j)
                if (valueAt != null) {
                    sbf.append(valueAt)
                }
                if (j < numcols - 1) sbf.append("\t")
            }

            val stringSelection = StringSelection(sbf.toString())
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            clipboard.setContents(stringSelection, null)
        }
    }
}