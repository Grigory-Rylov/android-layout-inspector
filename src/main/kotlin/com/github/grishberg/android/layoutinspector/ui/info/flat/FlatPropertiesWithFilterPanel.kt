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
            createViewPropertiesData(result, node)
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
        val viewFlags: ViewProperty?
        val privateFlags: ViewProperty?

        if (node is ViewNode) {
            widthProperty = node.getProperty("measurement:mMeasuredWidth") ?: node.getProperty("measuredWidth")
            heightProperty = node.getProperty("measurement:mMeasuredHeight") ?: node.getProperty("measuredHeight")
            viewFlags = node.getProperty("mViewFlags")
            privateFlags = node.getProperty("mPrivateFlags")

        } else {
            viewFlags = null
            privateFlags = null

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
        // add flags decoding
        val mViewFlags = hexStringToInt(viewFlags?.value)
        val mPrivateFlags = hexStringToInt(privateFlags?.value)

        if (mViewFlags != null && mPrivateFlags != null) {
            val stateProperty = ViewProperty(
                "view state",
                "state",
                category = null,
                value = decodeFlagsToState(mPrivateFlags, mViewFlags),
                isSizeProperty = false,
            )

            rows.add(
                RowInfoImpl(
                    property = stateProperty,
                    sizeInDp = false,
                    roundDp = false,
                    dpPerPixels = 0.0,
                    alterName = "state",
                    isSummary = true,
                )
            )

            val isLayoutRequestedProperty = ViewProperty(
                "isLayoutRequested",
                "isLayoutRequested",
                category = null,
                value = decodeIsLayoutRequested(mPrivateFlags),
                isSizeProperty = false,
            )

            rows.add(
                RowInfoImpl(
                    property = isLayoutRequestedProperty,
                    sizeInDp = false,
                    roundDp = false,
                    dpPerPixels = 0.0,
                    alterName = "isLayoutRequested",
                    isSummary = true,
                )
            )

        }

        result["Summary"] = rows
    }

    private fun createViewPropertiesData(result: MutableMap<String, List<RowInfoImpl>>, node: ViewNode) {
        val rows = mutableListOf<RowInfoImpl>()
        val viewFlags = node.getProperty("mViewFlags")
        val privateFlags = node.getProperty("mPrivateFlags")
        val privateFlags3 = node.getProperty("mPrivateFlags3")

        // add flags decoding
        val mViewFlags = hexStringToInt(viewFlags?.value)
        val mPrivateFlags = hexStringToInt(privateFlags?.value)
        val mPrivateFlags3 = hexStringToInt(privateFlags3?.value)

        if (mViewFlags != null && mPrivateFlags != null) {
            val visibilityValue = when (mViewFlags and VISIBILITY_MASK) {
                VISIBLE -> "visible"
                INVISIBLE -> "invisible"
                GONE -> "gone"
                else -> ""
            }
            rows.add(createRowInfo(fullName = "Visibility", value = visibilityValue))

            rows.add(
                createRowInfo(
                    fullName = "Focusable",
                    value = ((mViewFlags and FOCUSABLE) == FOCUSABLE)
                )
            )
            rows.add(
                createRowInfo(
                    fullName = "Enabled mask",
                    value = ((mViewFlags and ENABLED_MASK) == ENABLED)
                )
            )
            rows.add(
                createRowInfo(
                    fullName = "Draw mask",
                    value = ((mViewFlags and DRAW_MASK) == WILL_NOT_DRAW)
                )
            )
            rows.add(
                createRowInfo(
                    fullName = "Scrollbars hor",
                    value = ((mViewFlags and SCROLLBARS_HORIZONTAL) != 0)
                )
            )

            rows.add(
                createRowInfo(
                    fullName = "Scrollbars vert",
                    value = ((mViewFlags and SCROLLBARS_VERTICAL) != 0)
                )
            )

            rows.add(
                createRowInfo(
                    fullName = "Clickable",
                    value = ((mViewFlags and CLICKABLE) != 0)
                )
            )
            rows.add(
                createRowInfo(
                    fullName = "Long clickable",
                    value = ((mViewFlags and LONG_CLICKABLE) != 0)
                )
            )

            rows.add(
                createRowInfo(
                    fullName = "Context clickable",
                    value = ((mViewFlags and CONTEXT_CLICKABLE) != 0)
                )
            )

            rows.add(
                createRowInfo(
                    fullName = "Is root namspace",
                    value = ((mPrivateFlags and PFLAG_IS_ROOT_NAMESPACE) != 0)
                )
            )

            rows.add(
                createRowInfo(
                    fullName = "Focused",
                    value = ((mPrivateFlags and PFLAG_FOCUSED) != 0)
                )
            )


            rows.add(
                createRowInfo(
                    fullName = "Selected",
                    value = ((mPrivateFlags and PFLAG_SELECTED) != 0)
                )
            )

            rows.add(
                createRowInfo(
                    fullName = "Prepressed",
                    value = if ((mPrivateFlags and PFLAG_PREPRESSED) != 0)
                        "prepressed"
                    else if ((mPrivateFlags and PFLAG_PRESSED) != 0) "pressed" else ""
                )
            )

            rows.add(
                createRowInfo(
                    fullName = "Hovered",
                    value = ((mPrivateFlags and PFLAG_HOVERED) != 0)
                )
            )
            rows.add(
                createRowInfo(
                    fullName = "Activated",
                    value = ((mPrivateFlags and PFLAG_ACTIVATED) != 0)
                )
            )

            rows.add(
                createRowInfo(
                    fullName = "Invalidated",
                    value = ((mPrivateFlags and PFLAG_INVALIDATED) != 0)
                )
            )

            rows.add(
                createRowInfo(
                    fullName = "Dirty",
                    value = ((mPrivateFlags and PFLAG_DIRTY_MASK) != 0)
                )
            )

            rows.add(
                createRowInfo(
                    fullName = "isLayoutRequested",
                    value = (mPrivateFlags and PFLAG_FORCE_LAYOUT) == PFLAG_FORCE_LAYOUT
                )
            )

            mPrivateFlags3?.let {
                val isLaidOut = (mPrivateFlags3 and PFLAG3_IS_LAID_OUT) == PFLAG3_IS_LAID_OUT

                rows.add(
                    createRowInfo(
                        fullName = "isLaidOut",
                        value = isLaidOut
                    )
                )

                rows.add(
                    createRowInfo(
                        fullName = "isLayoutValid",
                        value = ((mPrivateFlags and PFLAG_FORCE_LAYOUT) == PFLAG_FORCE_LAYOUT) && isLaidOut
                    )
                )
            }
        }

        result["Flags"] = rows
    }

    private fun createRowInfo(
        fullName: String,
        value: Boolean,
        name: String? = null,
        category: String? = null,
        isSizeProperty: Boolean = false,
        isSummary: Boolean = false
    ): RowInfoImpl = createRowInfo(
        fullName = fullName, value = value.toString(), name = name,
        category = category,
        isSizeProperty = isSizeProperty,
        isSummary = isSummary
    )

    private fun createRowInfo(
        fullName: String,
        value: String,
        name: String? = null,
        category: String? = null,
        isSizeProperty: Boolean = false,
        isSummary: Boolean = false
    ): RowInfoImpl {
        val property = ViewProperty(
            fullName,
            name ?: fullName,
            category = category,
            value = value,
            isSizeProperty = isSizeProperty,
        )

        return RowInfoImpl(
            property = property,
            sizeInDp = false,
            roundDp = false,
            dpPerPixels = 0.0,
            alterName = fullName,
            isSummary = isSummary,
        )
    }

    private fun hexStringToInt(hex: String?): Int? {
        if (hex == null) return null
        val cleanedHex = hex.removePrefix("0x").removePrefix("0X")
        return cleanedHex.toIntOrNull(16)
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

    private fun decodeIsLayoutRequested(mPrivateFlags: Int): String {
        val value = (mPrivateFlags and PFLAG_FORCE_LAYOUT) == PFLAG_FORCE_LAYOUT
        return if (value) "true" else "false"
    }

    private fun decodeFlagsToState(mPrivateFlags: Int, mViewFlags: Int): String {
        val out = StringBuilder(256)
        when (mViewFlags and VISIBILITY_MASK) {
            VISIBLE -> out.append('V')
            INVISIBLE -> out.append('I')
            GONE -> out.append('G')
            else -> out.append('.')
        }
        out.append(if ((mViewFlags and FOCUSABLE) == FOCUSABLE) 'F' else '.')
        out.append(if ((mViewFlags and ENABLED_MASK) == ENABLED) 'E' else '.')
        out.append(if ((mViewFlags and DRAW_MASK) == WILL_NOT_DRAW) '.' else 'D')
        out.append(if ((mViewFlags and SCROLLBARS_HORIZONTAL) != 0) 'H' else '.')
        out.append(if ((mViewFlags and SCROLLBARS_VERTICAL) != 0) 'V' else '.')
        out.append(if ((mViewFlags and CLICKABLE) != 0) 'C' else '.')
        out.append(if ((mViewFlags and LONG_CLICKABLE) != 0) 'L' else '.')
        out.append(if ((mViewFlags and CONTEXT_CLICKABLE) != 0) 'X' else '.')
        out.append(' ')
        out.append(if ((mPrivateFlags and PFLAG_IS_ROOT_NAMESPACE) != 0) 'R' else '.')
        out.append(if ((mPrivateFlags and PFLAG_FOCUSED) != 0) 'F' else '.')
        out.append(if ((mPrivateFlags and PFLAG_SELECTED) != 0) 'S' else '.')
        if ((mPrivateFlags and PFLAG_PREPRESSED) != 0) {
            out.append('p')
        } else {
            out.append(if ((mPrivateFlags and PFLAG_PRESSED) != 0) 'P' else '.')
        }
        out.append(if ((mPrivateFlags and PFLAG_HOVERED) != 0) 'H' else '.')
        out.append(if ((mPrivateFlags and PFLAG_ACTIVATED) != 0) 'A' else '.')
        out.append(if ((mPrivateFlags and PFLAG_INVALIDATED) != 0) 'I' else '.')
        out.append(if ((mPrivateFlags and PFLAG_DIRTY_MASK) != 0) 'D' else '.')
        return out.toString()
    }

    private companion object {
        const val PFLAG3_IS_LAID_OUT: Int = 0x4
        const val PFLAG_FORCE_LAYOUT: Int = 0x00001000
        private const val PFLAG_PRESSED = 0x00004000
        const val WILL_NOT_DRAW: Int = 0x00000080
        const val VISIBLE: Int = 0x00000000
        const val INVISIBLE: Int = 0x00000004
        const val GONE: Int = 0x00000008
        const val VISIBILITY_MASK: Int = 0x0000000C
        const val FOCUSABLE: Int = 0x00000001

        const val ENABLED: Int = 0x00000000

        const val ENABLED_MASK: Int = 0x00000020
        const val DRAW_MASK: Int = 0x00000080
        const val SCROLLBARS_HORIZONTAL: Int = 0x00000100
        const val SCROLLBARS_VERTICAL: Int = 0x00000200
        const val CLICKABLE: Int = 0x00004000
        const val LONG_CLICKABLE: Int = 0x00200000
        const val CONTEXT_CLICKABLE: Int = 0x00800000

        const val PFLAG_IS_ROOT_NAMESPACE: Int = 0x00000008
        const val PFLAG_FOCUSED: Int = 0x00000002
        const val PFLAG_SELECTED: Int = 0x00000004
        private const val PFLAG_PREPRESSED = 0x02000000
        private const val PFLAG_HOVERED = 0x10000000
        const val PFLAG_ACTIVATED: Int = 0x40000000
        const val PFLAG_INVALIDATED: Int = -0x80000000
        const val PFLAG_DIRTY_MASK: Int = 0x00200000

    }
}
