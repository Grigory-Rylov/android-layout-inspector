package com.github.grishberg.android.layoutinspector.ui.dialogs.bookmarks

import com.android.layoutinspector.model.ViewNode
import com.github.grishberg.android.layoutinspector.ui.dialogs.CloseByEscapeDialog
import java.awt.BorderLayout
import java.awt.Color
import java.awt.event.ActionEvent
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.*
import javax.swing.border.EmptyBorder

class NewBookmarkDialog(
    private val owner: JFrame,
    private val selectedViewNode: ViewNode
) : CloseByEscapeDialog(owner, "New bookmark", true) {
    var result: BookmarkInfo? = null
        private set

    private var colorChooser: JColorChooser
    private var bookmarkName: JTextField
    private var selectedColor = Color(201, 137, 255, 117)


    init {
        val content = JPanel()
        content.border = EmptyBorder(4, 4, 4, 4)
        content.layout = BorderLayout()

        bookmarkName = JTextField(10)
        bookmarkName.addActionListener {
            closeAfterSuccess()
        }
        content.add(bookmarkName, BorderLayout.PAGE_START)
        colorChooser = JColorChooser(selectedColor)
        colorChooser.selectionModel.addChangeListener { selectedColor = colorChooser.color }
        content.add(colorChooser, BorderLayout.CENTER)

        val okButton = JButton("OK")
        getRootPane().defaultButton = okButton
        content.add(okButton, BorderLayout.PAGE_END)
        okButton.addActionListener(object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                closeAfterSuccess()
            }
        })
        contentPane = content
        defaultCloseOperation = DO_NOTHING_ON_CLOSE

        addComponentListener(object : ComponentAdapter() {
            override fun componentShown(ce: ComponentEvent) {
                bookmarkName.requestFocusInWindow()
            }
        })
        defaultCloseOperation = DO_NOTHING_ON_CLOSE
        pack()

    }

    private fun closeAfterSuccess() {
        result = BookmarkInfo(selectedViewNode, bookmarkName.text, selectedColor)
        clearAndHide()
    }

    private fun clearAndHide() {
        bookmarkName.isEnabled = true
        bookmarkName.text = null
        isVisible = false
    }

    fun showDialog() {
        result = null
        setLocationRelativeTo(owner)
        isVisible = true
    }

    fun showEditDialog(bookmarkInfo: BookmarkInfo) {
        bookmarkInfo.description?.let {
            bookmarkName.text = bookmarkInfo.description
        }

        bookmarkInfo.color?.let {
            colorChooser.color = it
        }
        setLocationRelativeTo(owner)
        isVisible = true
    }
}