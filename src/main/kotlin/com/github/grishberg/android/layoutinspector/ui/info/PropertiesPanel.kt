package com.github.grishberg.android.layoutinspector.ui.info

import com.github.grishberg.android.layoutinspector.domain.AbstractViewNode
import javax.swing.JComponent

interface PropertiesPanel {

    fun getComponent(): JComponent

    fun showProperties(node: AbstractViewNode)

    fun setSizeDpMode(enabled: Boolean)

    fun roundDp(enabled: Boolean)
}
