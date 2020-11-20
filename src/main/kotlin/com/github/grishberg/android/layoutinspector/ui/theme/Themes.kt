package com.github.grishberg.android.layoutinspector.ui.theme

import com.android.layoutinspector.common.AppLogger
import com.intellij.util.ui.UIUtil
import java.awt.Frame
import javax.swing.SwingUtilities
import javax.swing.UnsupportedLookAndFeelException


class Themes(
    private val owner: Frame,
    private val themeProxy: ThemeProxy,
    logger: AppLogger
) {
    val isDark: Boolean
        get() = UIUtil.isUnderDarcula()

    init {
        try {
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
        SwingUtilities.updateComponentTreeUI(owner)
        themeProxy.currentTheme = MaterialDarkColors()
    }

    private fun setLiteTheme(set: Boolean = false) {

        if (set) {
            themeProxy.currentTheme = MaterialLiteColors()
            return
        }
        SwingUtilities.updateComponentTreeUI(owner)
        themeProxy.currentTheme = MaterialLiteColors()
    }
}
