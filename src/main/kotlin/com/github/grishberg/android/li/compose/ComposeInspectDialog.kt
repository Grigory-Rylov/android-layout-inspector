package com.github.grishberg.android.li.compose

import com.github.grishberg.android.li.compose.model.ComposeCandidateNode
import com.github.grishberg.android.li.compose.model.ComposeHierarchySummary
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.treeStructure.Tree
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.SwingConstants
import javax.swing.border.EmptyBorder
import javax.swing.table.DefaultTableModel
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel

class ComposeInspectDialog(
    project: Project,
    private val summary: ComposeHierarchySummary,
    private val root: ComposeCandidateNode?,
) : DialogWrapper(project, true) {

    init {
        title = "Compose Hierarchy Summary"
        setOKButtonText("Close")
        init()
    }

    override fun createCenterPanel(): JComponent {
        val splitter = JBSplitter(false, 0.45f)
        splitter.firstComponent = buildLeft()
        splitter.secondComponent = buildRight()
        splitter.preferredSize = Dimension(960, 640)
        return splitter
    }

    override fun createActions(): Array<javax.swing.Action> {
        val copy = object : javax.swing.AbstractAction("Copy summary to clipboard") {
            override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                clipboard.setContents(StringSelection(summary.toJsonLike()), null)
            }
        }
        return arrayOf(copy, okAction)
    }

    private fun buildLeft(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.border = EmptyBorder(10, 10, 10, 10)

        val statsPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(headerLabel("Device:  ${summary.deviceName}"))
            add(headerLabel("Top package:  ${summary.packageName.ifEmpty { "<unknown>" }}"))
            add(headerLabel(" "))
            add(statLine("Total nodes", summary.totalNodes.toString()))
            add(statLine("Visible nodes", summary.visibleNodes.toString()))
            add(statLine("Compose roots (AndroidComposeView)", summary.composeRootCount.toString()))
            add(statLine("Compose candidates", summary.composeCandidateCount.toString()))
            add(statLine("Nodes with text", summary.textNodeCount.toString()))
            add(statLine("Nodes with content-description", summary.contentDescNodeCount.toString()))
            add(statLine("Nodes with resource-id", summary.resourceIdNodeCount.toString()))
        }

        val tree = buildCandidateTree()

        panel.add(statsPanel, BorderLayout.NORTH)
        panel.add(JBScrollPane(tree), BorderLayout.CENTER)
        return panel
    }

    private fun buildCandidateTree(): Tree {
        val rootNode = DefaultMutableTreeNode("Compose candidates")
        if (root != null) {
            collectCandidates(root, rootNode)
        }
        val tree = Tree(DefaultTreeModel(rootNode))
        tree.isRootVisible = true
        tree.showsRootHandles = true
        tree.cellRenderer = object : DefaultTreeCellRenderer() {
            override fun getTreeCellRendererComponent(
                tree: javax.swing.JTree?,
                value: Any?,
                sel: Boolean,
                expanded: Boolean,
                leaf: Boolean,
                row: Int,
                hasFocus: Boolean,
            ): java.awt.Component {
                val node = value as? DefaultMutableTreeNode
                val payload = node?.userObject
                val text = when (payload) {
                    is ComposeCandidateNode -> payload.displayName()
                    else -> payload?.toString() ?: ""
                }
                return super.getTreeCellRendererComponent(
                    tree, text, sel, expanded, leaf, row, hasFocus
                )
            }
        }
        for (i in 0 until tree.rowCount) tree.expandRow(i)
        return tree
    }

    private fun collectCandidates(node: ComposeCandidateNode, parent: DefaultMutableTreeNode) {
        if (!node.isComposeCandidate) {
            // For non-compose nodes we still descend so we don't lose context,
            // but we don't add them to the visible tree unless they have a
            // candidate inside.
            val tempParent = DefaultMutableTreeNode(node.className.substringAfterLast('.'))
            node.children.forEach { collectCandidates(it, tempParent) }
            if (tempParent.childCount > 0) parent.add(tempParent)
            return
        }
        val current = DefaultMutableTreeNode(node)
        node.children.forEach { collectCandidates(it, current) }
        parent.add(current)
    }

    private fun buildRight(): JComponent {
        val tabs = JBTabbedPane()

        val classesModel = DefaultTableModel(arrayOf("Class", "Count"), 0).apply {
            summary.classDistribution.forEach { addRow(arrayOf<Any>(it.className, it.count)) }
        }
        tabs.addTab("Top classes", JBScrollPane(readOnlyTable(classesModel)))

        val textsModel = DefaultTableModel(arrayOf("Text"), 0).apply {
            summary.topTexts.forEach { addRow(arrayOf<Any>(it)) }
        }
        tabs.addTab("Top texts", JBScrollPane(readOnlyTable(textsModel)))

        val descsModel = DefaultTableModel(arrayOf("Content description"), 0).apply {
            summary.topContentDescriptions.forEach { addRow(arrayOf<Any>(it)) }
        }
        tabs.addTab("Top content-desc", JBScrollPane(readOnlyTable(descsModel)))

        return tabs
    }

    private fun readOnlyTable(model: DefaultTableModel): JTable =
        object : JTable(model) {
            override fun isCellEditable(row: Int, column: Int): Boolean = false
        }

    private fun headerLabel(text: String): JComponent {
        val label = JBLabel(text)
        label.horizontalAlignment = SwingConstants.LEFT
        return label
    }

    private fun statLine(name: String, value: String): JComponent {
        val row = JPanel(BorderLayout())
        row.add(JBLabel("$name:  "), BorderLayout.WEST)
        row.add(JBLabel(value), BorderLayout.CENTER)
        return row
    }
}

private fun ComposeHierarchySummary.toJsonLike(): String = buildString {
    appendLine("{")
    appendLine("  \"deviceName\": \"$deviceName\",")
    appendLine("  \"packageName\": \"$packageName\",")
    appendLine("  \"totalNodes\": $totalNodes,")
    appendLine("  \"visibleNodes\": $visibleNodes,")
    appendLine("  \"composeRootCount\": $composeRootCount,")
    appendLine("  \"composeCandidateCount\": $composeCandidateCount,")
    appendLine("  \"textNodeCount\": $textNodeCount,")
    appendLine("  \"contentDescNodeCount\": $contentDescNodeCount,")
    appendLine("  \"resourceIdNodeCount\": $resourceIdNodeCount,")
    appendLine("  \"topClasses\": [")
    classDistribution.forEachIndexed { i, c ->
        val tail = if (i == classDistribution.lastIndex) "" else ","
        appendLine("    { \"class\": \"${c.className}\", \"count\": ${c.count} }$tail")
    }
    appendLine("  ]")
    append("}")
}
