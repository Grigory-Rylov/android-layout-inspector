package com.github.grishberg.android.layoutinspector.ui.tree

import java.awt.Color
import javax.swing.ImageIcon
import javax.swing.tree.DefaultTreeCellRenderer

/**
 * Renderer for default view item.
 */
class SimpleViewNodeRenderer : DefaultTreeCellRenderer(), TreeItem {
    override fun setForeground(textColor1: Color, textColor2: Color) {
        foreground = textColor1
    }

    override fun setIcon(newIcon: ImageIcon) {
        icon = newIcon
    }


    override fun setTitle(newText: String) {
        text = newText
    }
}