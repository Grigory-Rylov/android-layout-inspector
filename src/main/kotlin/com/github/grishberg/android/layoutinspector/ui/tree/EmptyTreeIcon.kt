package com.github.grishberg.android.layoutinspector.ui.tree

import java.awt.Component
import java.awt.Graphics
import javax.swing.Icon

class EmptyTreeIcon : Icon {
    override fun paintIcon(c: Component, g: Graphics?, x: Int, y: Int) = Unit
    override fun getIconHeight() = SIZE
    override fun getIconWidth() = SIZE

    companion object {
        const val SIZE = 0
    }
}
