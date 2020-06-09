package com.github.grishberg.android.layoutinspector.ui.menu

import com.github.grishberg.android.layoutinspector.settings.Settings
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.AbstractButton

class ChangeBooleanSettingsAction(
    private val settings: Settings,
    private val key: String
) : ActionListener {
    override fun actionPerformed(e: ActionEvent) {
        val aButton = e.source as AbstractButton
        val selected = aButton.model.isSelected
        settings.setBoolValue(key, selected)
    }
}