package com.github.grishberg.android.layoutinspector.ui.tree


import sun.swing.DefaultLookup
import java.awt.Color
import javax.swing.ImageIcon
import javax.swing.JTextPane
import javax.swing.UIManager
import javax.swing.text.AttributeSet
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants
import javax.swing.text.StyleContext


class TextViewRenderer : JTextPane() {
    private val iconTextGap = 4

    private var drawDashedFocusIndicator: Boolean = false
    private var drawsFocusBorderAroundIcon: Boolean = false
    private val selectionBorderColor: Color = UIManager.getColor("Tree.selectionBorderColor")
    private val selectionForeground: Color = UIManager.getColor("Tree.selectionForeground")
    private val selectionBackground: Color = UIManager.getColor("Tree.selectionBackground")
    private val textForeground: Color = UIManager.getColor("Tree.textForeground")
    private val textBackground: Color = UIManager.getColor("Tree.textBackground")
    // If drawDashedFocusIndicator is true, the following are used.

    private val textLabelColor = Color(0, 0, 0, 127)
    private val icon = createImageIcon("icons/text.png")

    private var selected = false

    private var isDropCell = false
    private var text1: String = ""
    private var text2: String = ""

    init {
        drawsFocusBorderAroundIcon = DefaultLookup.getBoolean(this, ui, "Tree.drawsFocusBorderAroundIcon", false)
        drawDashedFocusIndicator = DefaultLookup.getBoolean(this, ui, "Tree.drawDashedFocusIndicator", false)
        background = null
        setCaret(NoTextSelectionCaret(this))
        caret.isVisible = false
        setEditable(false)
    }


    fun setNameAndText(name: String, text: String) {
        setEditable(true)
        setText("")
        text1 = name
        text2 = text
        insertIcon(icon)
        appendSpace(this)
        appendToPane(this, name, Color.BLACK);
        appendToPane(this, "- \"$text\"", textLabelColor)
        caretPosition = 0
        setEditable(false)

    }

    fun setSelected(sel: Boolean) {
        selected = sel
        setNameAndText(text1, text2)
    }

    private fun appendToPane(tp: JTextPane, msg: String, c: Color) {
        val sc = StyleContext.getDefaultStyleContext()
        val color = if (selected) selectionForeground else c
        var aset: AttributeSet? = sc.addAttribute(SimpleAttributeSet.EMPTY, StyleConstants.Foreground, color)
        if (selected) {
            aset = sc.addAttribute(aset, StyleConstants.Background, selectionBackground)
        }
        aset = sc.addAttribute(aset, StyleConstants.Alignment, StyleConstants.ALIGN_LEFT)
        val len = tp.document.length
        tp.caretPosition = len
        tp.setCharacterAttributes(aset, false)
        tp.replaceSelection(msg)
    }

    private fun appendSpace(tp: JTextPane) {
        val len = tp.document.length
        tp.caretPosition = len
        tp.setCharacterAttributes(SimpleAttributeSet.EMPTY, false)
        tp.replaceSelection(" ")
    }

    /** Returns an ImageIcon, or null if the path was invalid.  */
    private fun createImageIcon(path: String): ImageIcon? {
        val imgURL = ClassLoader.getSystemResource(path)
        return if (imgURL != null) {
            ImageIcon(imgURL)
        } else {
            System.err.println("Couldn't find file: $path")
            null
        }
    }
}