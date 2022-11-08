package com.github.grishberg.android.layoutinspector.ui

import com.github.grishberg.android.layoutinspector.domain.Logic
import com.github.grishberg.android.layoutinspector.ui.layout.LayoutPanel
import java.awt.KeyboardFocusManager
import java.awt.Toolkit
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.AbstractAction
import javax.swing.JComponent
import javax.swing.JTextField
import javax.swing.KeyStroke

class KeyBinder(
    keyBinderComponent: JComponent,
    private val layoutPanel: LayoutPanel,
    private val logic: Logic,
    private val main: Main
) {
    val condition = JComponent.WHEN_IN_FOCUSED_WINDOW
    val inputMap = keyBinderComponent.getInputMap(condition)
    val actionMap = keyBinderComponent.actionMap
    private val keyboardFocusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager()

    init {
        //addKeyMapWithCtrl(KeyEvent.VK_C, CopySelectedFullClassNameAction())
        //addKeyMap(KeyEvent.VK_ESCAPE, RemoveSelectionAction())
        addKeyMapWithCtrl(KeyEvent.VK_O, OpenFileDialogAction(false))
        addKeyMapWithCtrl(KeyEvent.VK_R, RefreshLayoutAction())
        addKeyMapWithCtrlShift(KeyEvent.VK_O, OpenFileDialogAction(true))

        addKeyMapWithCtrl(KeyEvent.VK_N, NewTraceAction())
        addKeyMap(KeyEvent.VK_Z, ResetZoomAction())
        addKeyMap(KeyEvent.VK_F, FitZoomAction())
        addKeyMap(KeyEvent.VK_L, ToggleLayoutAction())
        addKeyMapWithCtrl(KeyEvent.VK_F, GoToFindAction())
    }

    private fun addKeyMapWithCtrl(keyCode: Int, action: AbstractAction) {
        addKeyMap(keyCode, Toolkit.getDefaultToolkit().menuShortcutKeyMask, action)
    }

    private fun addKeyMapWithCtrlShift(keyCode: Int, action: AbstractAction) {
        addKeyMap(keyCode, Toolkit.getDefaultToolkit().menuShortcutKeyMask + ActionEvent.SHIFT_MASK, action)
    }

    private fun addKeyMapWithCtrlAlt(keyCode: Int, action: AbstractAction) {
        addKeyMap(keyCode, Toolkit.getDefaultToolkit().menuShortcutKeyMask + ActionEvent.ALT_MASK, action)
    }

    private fun addKeyMap(keyCode: Int, action: AbstractAction) {
        val keyStroke: KeyStroke = KeyStroke.getKeyStroke(keyCode, 0)
        inputMap.put(keyStroke, keyStroke.toString())
        actionMap.put(keyStroke.toString(), action)
    }


    private fun addKeyMap(keyCode: Int, modifiers: Int, action: AbstractAction) {
        val keyStroke: KeyStroke = KeyStroke.getKeyStroke(keyCode, modifiers)
        inputMap.put(keyStroke, keyStroke.toString())
        actionMap.put(keyStroke.toString(), action)
    }

    private fun shouldSkip(e: ActionEvent): Boolean {
        val focused = keyboardFocusManager.focusOwner
        if (focused is JTextField) {
            return true
        }
        return false
    }

    private inner class OpenFileDialogAction(private val inNewWindow: Boolean) : AbstractAction() {
        override fun actionPerformed(e: ActionEvent) {
            if (shouldSkip(e)) return
            main.openExistingFile(inNewWindow)
        }
    }

    private inner class NewTraceAction : AbstractAction() {
        override fun actionPerformed(e: ActionEvent) {
            if (shouldSkip(e)) return
            logic.startRecording()
        }
    }
    private inner class RefreshLayoutAction : AbstractAction() {
        override fun actionPerformed(e: ActionEvent) {
            if (shouldSkip(e)) return
            logic.refreshLayout()
        }
    }

    private inner class FitZoomAction : AbstractAction() {
        override fun actionPerformed(e: ActionEvent) {
            if (shouldSkip(e)) return
            layoutPanel.fitZoom()
        }
    }

    private inner class ToggleLayoutAction : AbstractAction() {
        override fun actionPerformed(e: ActionEvent) {
            if (shouldSkip(e)) return
            main.toggleShowingLayouts()
        }
    }

    private inner class ResetZoomAction : AbstractAction() {
        override fun actionPerformed(e: ActionEvent) {
            if (shouldSkip(e)) return
            layoutPanel.resetZoom()
        }
    }


    private inner class GoToFindAction : AbstractAction() {
        override fun actionPerformed(e: ActionEvent) {
            if (shouldSkip(e)) return
            main.showFindDialog()
        }
    }
}
