package com.github.grishberg.android.layoutinspector.ui.dialogs

import com.github.grishberg.android.layoutinspector.settings.SettingsFacade
import java.awt.FlowLayout
import javax.swing.*
import javax.swing.text.DefaultEditorKit
import javax.swing.text.DefaultEditorKit.CutAction
import javax.swing.text.DefaultEditorKit.PasteAction


/**
 * Enter ANDROID_HOME.
 */
class SetAndroidHomeDialog(
    owner: JFrame,
    private val settings: SettingsFacade
) : CloseByEscapeDialog(owner, "Set path to Android SDK", true) {
    private val androidHomeField = JTextField(25)

    init {
        val menu = JPopupMenu()
        val cut: Action = CutAction()
        cut.putValue(Action.NAME, "Cut")
        cut.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control X"))
        menu.add(cut)

        val copy: Action = DefaultEditorKit.CopyAction()
        copy.putValue(Action.NAME, "Copy")
        copy.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control C"))
        menu.add(copy)

        val paste: Action = PasteAction()
        paste.putValue(Action.NAME, "Paste")
        paste.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control V"))
        menu.add(paste)

        androidHomeField.setComponentPopupMenu(menu)

        val content = JPanel()
        content.layout = FlowLayout()

        content.add(JLabel("Android SDK (ANDROID_HOME)"))
        content.add(androidHomeField)

        val okButton = JButton("OK")
        okButton.addActionListener { storeValueAndClose() }
        content.add(okButton)
        contentPane = content
        androidHomeField.text = settings.androidHome
        pack()
    }

    private fun storeValueAndClose() {
        val androidHome = androidHomeField.text.trim()
        if (androidHome.isEmpty()) {
            return
        }
        settings.androidHome = androidHome
        isVisible = false
    }
}
