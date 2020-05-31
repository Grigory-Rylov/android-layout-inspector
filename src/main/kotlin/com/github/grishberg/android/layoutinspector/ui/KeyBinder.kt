package com.github.grishberg.android.layoutinspector.ui

import java.awt.KeyboardFocusManager
import java.awt.Toolkit
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.AbstractAction
import javax.swing.JComponent
import javax.swing.JTextField
import javax.swing.KeyStroke

class KeyBinder(
    keyBinderComponent: JComponent
) {
    val condition = JComponent.WHEN_IN_FOCUSED_WINDOW
    val inputMap = keyBinderComponent.getInputMap(condition)
    val actionMap = keyBinderComponent.actionMap
    private val keyboardFocusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager()

    init {
        //addKeyMapWithCtrl(KeyEvent.VK_C, CopySelectedFullClassNameAction())
        //addKeyMapWithCtrl(KeyEvent.VK_F, GoToFindAction())
        //addKeyMap(KeyEvent.VK_ESCAPE, RemoveSelectionAction())
        //addKeyMapWithCtrl(KeyEvent.VK_O, OpenFileDialogAction())
        //addKeyMapWithCtrl(KeyEvent.VK_N, NewTraceAction())
        //addKeyMap(KeyEvent.VK_Z, ResetZoomAction())
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
}