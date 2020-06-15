package com.github.grishberg.android.layoutinspector.ui.tree

import java.awt.Color
import javax.swing.ImageIcon

interface TreeItem {
    fun setForeground(textColor1: Color, textColor2: Color)
    fun setIcon(newIcon: ImageIcon)
    fun setBackgroundSelectionColor(color: Color)

    fun prepareTreeItem(
        type: String, description: String = "",
        sel: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        hovered: Boolean,
        hasFocus: Boolean = false
    )
}
