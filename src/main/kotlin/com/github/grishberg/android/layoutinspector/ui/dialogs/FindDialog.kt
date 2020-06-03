package com.github.grishberg.android.layoutinspector.ui.dialogs

import com.android.layoutinspector.model.ViewNode
import com.github.grishberg.android.layoutinspector.ui.tree.TreePanel
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Toolkit
import javax.swing.*

private const val TOP_OFFSET = 16

class FindDialog(
    private val owner: JFrame,
    private val treePanel: TreePanel
): JDialog(owner, "Find") {

    private val findField : JTextField
    private val prevButton: JButton
    private val nextButton: JButton
    private val resultLabel: JLabel
    private val checkboxOnlyInId: JCheckBox
    private val results = mutableListOf<ViewNode>()

    var rootNode: ViewNode? = null

    init {
        val content = JPanel()
        content.layout = FlowLayout()
        findField = JTextField(20)
        findField.toolTipText = "Input text and press <Enter>"
        findField.addActionListener {
            find()
        }

        checkboxOnlyInId = JCheckBox("Only ID")

        prevButton = JButton("<")
        prevButton.addActionListener {
            prev()
        }
        nextButton = JButton(">")
        nextButton.addActionListener {
            next()
        }
        resultLabel = JLabel()

        content.add(findField)
        content.add(checkboxOnlyInId)
        content.add(prevButton)
        content.add(nextButton)
        content.add(resultLabel)

        contentPane = content
        pack()
    }

    private fun find() {
        val text = findField.text
        if (text.isEmpty()) {
            return
        }
        results.clear()


    }

    private fun prev() {

    }

    private fun next() {

    }

    fun showDialog() {
        val toolkit: Toolkit = Toolkit.getDefaultToolkit()
        val screenSize: Dimension = toolkit.screenSize
        val x = (screenSize.width - owner.width) / 2
        val y = TOP_OFFSET
        setLocation(x, y)
        isVisible = true
    }
}