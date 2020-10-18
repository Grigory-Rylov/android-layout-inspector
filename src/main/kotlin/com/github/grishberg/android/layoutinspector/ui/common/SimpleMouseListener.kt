package com.github.grishberg.android.layoutinspector.ui.common

import java.awt.event.MouseEvent
import java.awt.event.MouseListener

abstract class SimpleMouseListener : MouseListener {
    override fun mouseReleased(e: MouseEvent) = Unit

    override fun mouseEntered(e: MouseEvent) = Unit

    override fun mouseExited(e: MouseEvent) = Unit

    override fun mousePressed(e: MouseEvent) = Unit
}