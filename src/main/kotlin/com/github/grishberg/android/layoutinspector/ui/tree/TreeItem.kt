package com.github.grishberg.android.layoutinspector.ui.tree

import java.awt.Color
import javax.swing.ImageIcon

interface TreeItem {
    fun setForeground(textColor1: Color, textColor2: Color)
    fun setIcon(icon: ImageIcon)
    fun setTitle(text: String) = Unit
    fun setTitle(type: String, description: String) = Unit
}