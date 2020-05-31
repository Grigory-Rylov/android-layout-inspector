package com.github.grishberg.android.layoutinspector.ui.tree

import java.awt.Color
import javax.swing.text.DefaultCaret
import javax.swing.text.JTextComponent

class NoTextSelectionCaret(textComponent: JTextComponent) : DefaultCaret() {
    override fun getMark(): Int {
        return dot
    }

    init {
        textComponent.highlighter = null
        textComponent.caretColor = Color(0, 0, 0, 0)
    }
}