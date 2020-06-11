package com.github.grishberg.android.layoutinspector.ui

import com.android.layoutinspector.common.AppLogger
import com.github.grishberg.android.layoutinspector.settings.SettingsFacade
import com.github.grishberg.android.layoutinspector.ui.theme.MaterialDarkColors
import com.github.grishberg.android.layoutinspector.ui.theme.MaterialLiteColors
import com.github.grishberg.android.layoutinspector.ui.theme.MaterialOceanColors
import com.github.grishberg.android.layoutinspector.ui.theme.ThemeProxy
import mdlaf.MaterialLookAndFeel
import mdlaf.themes.JMarsDarkTheme
import mdlaf.themes.MaterialLiteTheme
import mdlaf.themes.MaterialOceanicTheme
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
                Theme.OCEANIC -> setOceanicTheme(true)
                Theme.LITE -> setLiteTheme(true)
                Theme.DARK -> setDarkTheme(true)
            }
        } catch (e: UnsupportedLookAndFeelException) {
            logger.e("Error while switching theme", e)
        }
    }

    fun createThemeMenu(): JMenu {
        val menuTheme = JMenu("Themes")
        /*
        // TODO: enable oceanic theme
        val oceanic = JMenuItem()
        oceanic.action = object : AbstractAction("Oceanic") {
            override fun actionPerformed(e: ActionEvent) {
                setOceanicTheme()
            }
        }
        menuTheme.add(oceanic)
        */
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

        return menuTheme
    }

    private fun setDarkTheme(set: Boolean = false) {
        val jMarsDarkTheme = JMarsDarkTheme()
        if (set) {
            UIManager.setLookAndFeel(MaterialLookAndFeel(jMarsDarkTheme))
            themeProxy.currentTheme = MaterialDarkColors()
            return
        }
        settings.theme = Theme.DARK
        MaterialLookAndFeel.changeTheme(jMarsDarkTheme)
        SwingUtilities.updateComponentTreeUI(owner)
        themeProxy.currentTheme = MaterialDarkColors()
    }

    private fun setLiteTheme(set: Boolean = false) {
        val materialLiteTheme = MaterialLiteTheme()

        if (set) {
            UIManager.setLookAndFeel(MaterialLookAndFeel(materialLiteTheme))
            themeProxy.currentTheme = MaterialLiteColors()
            return
        }
        settings.theme = Theme.LITE
        MaterialLookAndFeel.changeTheme(materialLiteTheme)
        SwingUtilities.updateComponentTreeUI(owner)
        themeProxy.currentTheme = MaterialLiteColors()
    }

    private fun setOceanicTheme(set: Boolean = false) {
        val materialOceanicTheme = MaterialOceanicTheme()

        if (set) {
            UIManager.setLookAndFeel(MaterialLookAndFeel(materialOceanicTheme))
            themeProxy.currentTheme = MaterialOceanColors()
            return
        }
        settings.theme = Theme.OCEANIC
        MaterialLookAndFeel.changeTheme(materialOceanicTheme)
        SwingUtilities.updateComponentTreeUI(owner)
        themeProxy.currentTheme = MaterialOceanColors()
    }
}
