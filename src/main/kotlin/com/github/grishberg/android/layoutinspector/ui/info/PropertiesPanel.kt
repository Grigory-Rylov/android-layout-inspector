package com.github.grishberg.android.layoutinspector.ui.info

import com.android.layoutinspector.model.ViewNode
import javax.swing.JComponent

interface PropertiesPanel {
    fun getComponent(): JComponent

    fun showProperties(node: ViewNode)

    fun setSizeDpMode(enabled: Boolean)

    fun roundDp(enabled: Boolean)
}