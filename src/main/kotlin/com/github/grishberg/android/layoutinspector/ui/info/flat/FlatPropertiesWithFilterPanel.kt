package com.github.grishberg.android.layoutinspector.ui.info.flat

import com.android.layoutinspector.common.AppLogger
import com.android.layoutinspector.model.ViewNode
import com.android.layoutinspector.model.ViewProperty
import com.github.grishberg.android.layoutinspector.domain.AbstractViewNode
import com.github.grishberg.android.layoutinspector.domain.MetaRepository
import com.github.grishberg.android.layoutinspector.settings.SettingsFacade
import com.github.grishberg.android.layoutinspector.ui.info.PropertiesPanel
import com.github.grishberg.android.layoutinspector.ui.info.RowInfoImpl
import com.github.grishberg.android.layoutinspector.ui.info.flat.filter.FilterView
import com.github.grishberg.android.layoutinspector.ui.info.flat.filter.PropertiesTableFilter
import com.github.grishberg.android.layoutinspector.ui.theme.ThemeColors
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.KeyEvent
import java.util.regex.PatternSyntaxException
import javax.swing.*
import javax.swing.table.TableRowSorter

private const val BORDER_SIZE = 4

class FlatPropertiesWithFilterPanel(
    private val meta: MetaRepository,
    settings: SettingsFacade,
    themeColors: ThemeColors,
    private val filterView: FilterView,
    private val logger: AppLogger,
) : JPanel(), PropertiesPanel {

    private var currentNode: AbstractViewNode? = null
    private var sorter: TableRowSorter<FlatGroupTableModel>
    private val table: JTable
    private var sizeInDp = false
    private var shouldRoundDp = settings.roundDimensions
    private val model = FlatGroupTableModel(emptyMap())

    init {
        layout = BorderLayout()

        table = JBTable(model)
        table.autoResizeMode = JTable.AUTO_RESIZE_LAST_COLUMN

        sorter = TableRowSorter(model)
        table.rowSorter = sorter

        val scrollPane = JBScrollPane(table)
        scrollPane.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS
        scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_ALWAYS

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
            KeyEvent.VK_C, Toolkit.getDefaultToolkit().menuShortcutKeyMask, false
        )
        table.registerKeyboardAction(CopyAction(), "Copy", copyStroke, JComponent.WHEN_FOCUSED)

        add(scrollPane, BorderLayout.CENTER)
        add(filterView.component, BorderLayout.NORTH)

        filterView.setOnTextChangedListener {
            filter(it)
        }

        this.preferredSize = Dimension(
            table.preferredSize.width, table.preferredSize.height + 85
        )
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

    override fun showProperties(node: AbstractViewNode) {
        currentNode = node
        val createPropertiesData = createPropertiesData(node)
        model.updateData(createPropertiesData)
        if (filterView.filterText.isNotEmpty()) {
            filter(filterView.filterText)
        }

        updateRowsHeight()
    }

    private fun updateRowsHeight() {
        val itemHeight = ui.getPreferredSize(this)?.height ?: 36
        val visibleRowsCount = sorter.viewRowCount
        for (row in 0 until visibleRowsCount) {
            val viewRow = table.convertRowIndexToModel(row)
            if (viewRow < 0) {
                continue
            }

            when (table.model.getValueAt(viewRow, 0)) {
                is TableValue.Empty, is TableValue.Header -> {
                    table.setRowHeight(row, itemHeight + BORDER_SIZE * 2)
                }

                else -> {
                    table.setRowHeight(row, itemHeight)
                }
            }
        }
    }

    private fun createPropertiesData(node: AbstractViewNode): Map<String, List<RowInfoImpl>> {
        val result = mutableMapOf<String, List<RowInfoImpl>>()
        createSummary(result, node)
        if (node is ViewNode) {
            for (entry in node.groupedProperties) {
                val rows = mutableListOf<RowInfoImpl>()
                for (property in entry.value) {
                    rows.add(RowInfoImpl(property, sizeInDp, shouldRoundDp, meta.dpPerPixels))
                }
                result[entry.key] = rows
            }
        } else {
            val rows = mutableListOf<RowInfoImpl>()
            rows.add(
                RowInfoImpl(
                    ViewProperty("Name", "Name", category = null, node.name),
                    sizeInDp, shouldRoundDp, meta.dpPerPixels
                )
            )
            rows.add(
                RowInfoImpl(
                    ViewProperty("Type", "Type", category = null, node.typeAsString),
                    sizeInDp, shouldRoundDp, meta.dpPerPixels
                )
            )
            if (!node.id.isNullOrEmpty()) {
                rows.add(
                    RowInfoImpl(
                        ViewProperty("ID", "ID", category = null, node.id!!),
                        sizeInDp, shouldRoundDp, meta.dpPerPixels
                    )
                )
            }
            if (!node.text.isNullOrEmpty()) {
                rows.add(
                    RowInfoImpl(
                        ViewProperty("Text", "Text", category = null, node.text!!),
                        sizeInDp, shouldRoundDp, meta.dpPerPixels
                    )
                )
            }
            result["Attributes"] = rows
        }
        return result
    }

    private fun createSummary(result: MutableMap<String, List<RowInfoImpl>>, node: AbstractViewNode) {
        val widthProperty: ViewProperty?
        val heightProperty: ViewProperty?
        if (node is ViewNode) {
            widthProperty = node.getProperty("measurement:mMeasuredWidth") ?: node.getProperty("measuredWidth")
            heightProperty = node.getProperty("measurement:mMeasuredHeight") ?: node.getProperty("measuredHeight")

        } else {
            widthProperty = ViewProperty(
                "width",
                "width",
                category = null,
                node.width.toString(),
                isSizeProperty = true,
                node.width
            )
            heightProperty = ViewProperty(
                "height",
                "height",
                category = null,
                node.height.toString(),
                isSizeProperty = true,
                node.height
            )

        }
        val xProperty = ViewProperty(
            "x",
            "x",
            category = null,
            node.locationOnScreenX.toString(),
            isSizeProperty = false,
            node.locationOnScreenX
        )
        val yProperty = ViewProperty(
            "y",
            "y",
            category = null,
            node.locationOnScreenY.toString(),
            isSizeProperty = false,
            node.locationOnScreenY
        )

        val rows = mutableListOf<RowInfoImpl>()
        rows.add(
            RowInfoImpl(
                property = xProperty,
                sizeInDp = sizeInDp,
                roundDp = shouldRoundDp,
                dpPerPixels = meta.dpPerPixels,
                alterName = "x",
                isSummary = true,
            )
        )
        rows.add(
            RowInfoImpl(
                property = yProperty,
                sizeInDp = sizeInDp,
                roundDp = shouldRoundDp,
                dpPerPixels = meta.dpPerPixels,
                alterName = "y",
                isSummary = true
            )
        )
        widthProperty?.let {
            rows.add(
                RowInfoImpl(
                    property = it, sizeInDp = sizeInDp,
                    roundDp = shouldRoundDp,
                    dpPerPixels = meta.dpPerPixels,
                    alterName = "width",
                    isSummary = true,
                )
            )
        }
        heightProperty?.let {
            rows.add(
                RowInfoImpl(
                    property = it, sizeInDp = sizeInDp,
                    roundDp = shouldRoundDp,
                    dpPerPixels = meta.dpPerPixels,
                    alterName = "height",
                    isSummary = true,
                )
            )
        }

        result["Summary"] = rows
    }

    override fun setSizeDpMode(enabled: Boolean) {
        val shouldInvalidate = sizeInDp != enabled
        sizeInDp = enabled
        if (shouldInvalidate) {
            currentNode?.let {
                showProperties(it)
            }
        }
    }

    override fun roundDp(enabled: Boolean) {
        val shouldInvalidate = shouldRoundDp != enabled
        shouldRoundDp = enabled
        if (shouldInvalidate && sizeInDp) {
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
                    null, "Invalid Copy Selection", "Invalid Copy Selection", JOptionPane.ERROR_MESSAGE
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
