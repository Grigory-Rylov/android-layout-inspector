package com.github.grishberg.android.layoutinspector.ui.dialogs

import com.android.layoutinspector.model.ViewNode
import java.awt.FlowLayout
import javax.swing.*

private const val TOP_OFFSET = 16
private const val FIND_LABEL_DEFAULT_TEXT = "results not found"

class FindDialog(
    private val owner: JFrame
) : CloseByEscapeDialog(owner, "Find") {

    private val findField: JTextField
    private val prevButton: JButton
    private val nextButton: JButton
    private val resultLabel: JLabel
    private val checkboxOnlyInId: JCheckBox
    private var onlyId = false
    private var onlyName = false
    private val results = mutableListOf<ViewNode>()
    private var currentIndex = 0
    private var searchText = ""

    private var rootNode: ViewNode? = null
    var foundAction: OnFoundAction? = null

    init {
        val content = JPanel()
        content.layout = FlowLayout()
        findField = JTextField(20)
        findField.toolTipText = "Input text and press <Enter>"
        findField.addActionListener {
            find()
        }

        checkboxOnlyInId = JCheckBox("Only ID")
        checkboxOnlyInId.addItemListener { e ->
            onlyId = e.stateChange == 1
        }

        prevButton = JButton("<")
        prevButton.addActionListener {
            prev()
        }
        nextButton = JButton(">")
        nextButton.addActionListener {
            next()
        }
        resultLabel = JLabel(FIND_LABEL_DEFAULT_TEXT)

        content.add(findField)
        content.add(checkboxOnlyInId)
        content.add(prevButton)
        content.add(nextButton)
        content.add(resultLabel)

        contentPane = content
        pack()
    }

    override fun onDialogClosed() {
        foundAction?.onFoundDialogClosed()
    }

    private fun find() {
        val text = findField.text
        if (text.isEmpty()) {
            return
        }
        searchText = text.toLowerCase()
        results.clear()
        resultLabel.text = FIND_LABEL_DEFAULT_TEXT
        currentIndex = 0

        rootNode?.let {
            processChildren(it)
        }

        if (results.size > 0) {
            foundAction?.onFound(results)
        }
        updateLabelAndNavigateToTree()
    }

    private fun prev() {
        if (results.isEmpty()) {
            return
        }
        currentIndex--
        if (currentIndex < 0) {
            currentIndex = results.size - 1
        }
        updateLabelAndNavigateToTree()
    }

    private fun next() {
        if (results.isEmpty()) {
            return
        }
        currentIndex++
        if (currentIndex > results.size - 1) {
            currentIndex = 0
        }
        updateLabelAndNavigateToTree()
    }

    private fun updateLabelAndNavigateToTree() {
        if (results.size > 0) {
            resultLabel.text = "found $currentIndex / ${results.size}"
            foundAction?.onSelectedFoundItem(results[currentIndex])
        }
    }

    private fun processChildren(node: ViewNode) {
        if (isSuitable(node)) {
            results.add(node)
        }

        val count = node.childCount
        for (i in 0 until count) {
            processChildren(node.getChildAt(i))
        }
    }

    private fun isSuitable(node: ViewNode): Boolean {
        if (onlyId) {
            if (node.id != null) {
                return node.id!!.contains(searchText, true)
            }
        } else if (onlyName) {
            return node.name.contains(searchText, true)
        }

        if (node.name.contains(searchText, true)) {
            return true
        }
        if (node.id != null) {
            return node.id!!.contains(searchText, true)
        }
        return false
    }

    fun updateRootNode(root: ViewNode?) {
        rootNode = root
        resultLabel.text = FIND_LABEL_DEFAULT_TEXT
        results.clear()
        currentIndex = 0
    }

    fun showDialog() {
        val x = (owner.width) / 2 - (width / 2)
        val y = TOP_OFFSET
        setLocation(x, y)
        isVisible = true
    }

    interface OnFoundAction {
        fun onFound(foundItems: List<ViewNode>)
        fun onSelectedFoundItem(node: ViewNode)
        fun onFoundDialogClosed()
    }
}