package com.github.grishberg.android.layoutinspector.ui.theme

import com.android.layoutinspector.common.AppLogger
import com.github.grishberg.android.layoutinspector.settings.SettingsFacade
import com.intellij.util.ui.UIUtil
import java.awt.Frame
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JMenuItem
import javax.swing.SwingUtilities
import javax.swing.UnsupportedLookAndFeelException

enum class Theme {
    LITE,
    DARK
}

class Themes(
    private val owner: Frame,
    private val settings: SettingsFacade,
    private val themeProxy: ThemeProxy,
    logger: AppLogger
) {
    init {
        try {
            val isDark = UIUtil.isUnderDarcula()
            if (isDark) {
                setDarkTheme()
            } else {
                setLiteTheme()
            }
        } catch (e: UnsupportedLookAndFeelException) {
            logger.e("Error while switching theme", e)
        }
    }

    private fun setDarkTheme(set: Boolean = false) {
        if (set) {
            themeProxy.currentTheme = MaterialDarkColors()
            return
        }
        settings.theme = Theme.DARK
        SwingUtilities.updateComponentTreeUI(owner)
        themeProxy.currentTheme = MaterialDarkColors()
    }

    private fun setLiteTheme(set: Boolean = false) {

        if (set) {
            themeProxy.currentTheme = MaterialLiteColors()
            return
        }
        settings.theme = Theme.LITE
        SwingUtilities.updateComponentTreeUI(owner)
        themeProxy.currentTheme = MaterialLiteColors()
    }
}
