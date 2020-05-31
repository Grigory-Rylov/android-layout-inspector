package com.github.grishberg.android.layoutinspector.ui.common

import java.awt.event.MouseEvent
import java.awt.event.MouseMotionListener

abstract class SimpleMouseMotionListener : MouseMotionListener {
    override fun mouseDragged(e: MouseEvent) = Unit
}