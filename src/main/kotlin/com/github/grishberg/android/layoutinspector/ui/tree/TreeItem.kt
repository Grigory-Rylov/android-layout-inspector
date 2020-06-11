package com.github.grishberg.android.layoutinspector.ui.tree

import java.awt.Color
import javax.swing.ImageIcon

interface TreeItem {
    var leaf: Boolean
    var expanded: Boolean
    var hovered: Boolean
    var selected: Boolean

    fun setForeground(textColor1: Color, textColor2: Color)
    fun setIcon(newIcon: ImageIcon)
    fun setTitle(type: String, description: String = "")
    fun setBackgroundSelectionColor(color: Color)
}