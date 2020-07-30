package com.github.grishberg.android.layoutinspector.ui.theme

import com.android.layoutinspector.common.AppLogger
import com.formdev.flatlaf.FlatDarculaLaf
import com.formdev.flatlaf.FlatLightLaf
import com.github.grishberg.android.layoutinspector.settings.SettingsFacade
import java.awt.Frame
import java.awt.event.ActionEvent
import javax.swing.*

enum class Theme {
    OCEANIC,
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
            JDialog.setDefaultLookAndFeelDecorated(true)
            val themeName = settings.theme
            when (themeName) {
                Theme.DARK -> setDarkTheme(true)
                else -> setLiteTheme(true)
            }
        } catch (e: UnsupportedLookAndFeelException) {
            logger.e("Error while switching theme", e)
        }
    }

    fun createThemeMenu(menuBar: JMenuBar) {
        val menuTheme = JMenu("Themes")

        val lite = JMenuItem()
        lite.action = object : AbstractAction("Lite") {
            override fun actionPerformed(e: ActionEvent) {
                setLiteTheme()
            }
        }
        menuTheme.add(lite)

        val jmarsDark = JMenuItem()
        jmarsDark.action = object : AbstractAction("Dark") {
            override fun actionPerformed(e: ActionEvent) {
                setDarkTheme()
            }
        }
        menuTheme.add(jmarsDark)

        menuBar.add(menuTheme)
    }

    private fun setDarkTheme(set: Boolean = false) {
        if (set) {
            UIManager.setLookAndFeel(FlatDarculaLaf())
            themeProxy.currentTheme = MaterialDarkColors()
            return
        }
        settings.theme = Theme.DARK
        UIManager.setLookAndFeel(FlatDarculaLaf());
        SwingUtilities.updateComponentTreeUI(owner)
        themeProxy.currentTheme = MaterialDarkColors()
    }

    private fun setLiteTheme(set: Boolean = false) {

        if (set) {
            UIManager.setLookAndFeel(FlatLightLaf())
            themeProxy.currentTheme = MaterialLiteColors()
            return
        }
        settings.theme = Theme.LITE
        UIManager.setLookAndFeel(FlatLightLaf())
        SwingUtilities.updateComponentTreeUI(owner)
        themeProxy.currentTheme = MaterialLiteColors()
    }
}
