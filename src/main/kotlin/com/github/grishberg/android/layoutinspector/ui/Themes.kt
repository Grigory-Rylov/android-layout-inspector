package com.github.grishberg.android.layoutinspector.ui

import com.android.layoutinspector.common.AppLogger
import com.github.grishberg.android.layoutinspector.settings.Settings
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

private const val SETTINGS_THEME = "theme"

class Themes(
    private val owner: Frame,
    private val settings: Settings,
    logger: AppLogger
) {
    init {
        try {
            JDialog.setDefaultLookAndFeelDecorated(true)
            val themeName = settings.getStringValueOrDefault(SETTINGS_THEME, Theme.OCEANIC.name)
            when (themeName) {
                Theme.OCEANIC.name -> setOceanicTheme(true)
                Theme.LITE.name -> setLiteTheme(true)
                Theme.DARK.name -> setDarkTheme(true)
            }
        } catch (e: UnsupportedLookAndFeelException) {
            logger.e("Error while switching theme", e)
        }
    }

    fun createThemeMenu(): JMenu {
        val menuTheme = JMenu("Themes")
        val oceanic = JMenuItem()
        oceanic.action = object : AbstractAction("Oceanic") {
            override fun actionPerformed(e: ActionEvent) {
                setOceanicTheme()
            }
        }
        val lite = JMenuItem()
        lite.action = object : AbstractAction("Lite") {
            override fun actionPerformed(e: ActionEvent) {
                setLiteTheme()
            }
        }
        val jmarsDark = JMenuItem()
        jmarsDark.action = object : AbstractAction("Dark") {
            override fun actionPerformed(e: ActionEvent) {
                setDarkTheme()
            }
        }

        menuTheme.add(oceanic)
        menuTheme.add(lite)
        menuTheme.add(jmarsDark)
        return menuTheme
    }

    private fun setDarkTheme(set: Boolean = false) {
        settings.setStringValue(SETTINGS_THEME, Theme.DARK.name)
        if (set) {
            UIManager.setLookAndFeel(MaterialLookAndFeel(JMarsDarkTheme()))
            return
        }
        MaterialLookAndFeel.changeTheme(JMarsDarkTheme())
        SwingUtilities.updateComponentTreeUI(owner)
    }

    private fun setLiteTheme(set: Boolean = false) {
        settings.setStringValue(SETTINGS_THEME, Theme.LITE.name)
        if (set) {
            UIManager.setLookAndFeel(MaterialLookAndFeel(MaterialLiteTheme()))
            return
        }
        MaterialLookAndFeel.changeTheme(MaterialLiteTheme())
        SwingUtilities.updateComponentTreeUI(owner)
    }

    private fun setOceanicTheme(set: Boolean = false) {
        settings.setStringValue(SETTINGS_THEME, Theme.OCEANIC.name)
        if (set) {
            UIManager.setLookAndFeel(MaterialLookAndFeel(MaterialOceanicTheme()))
            return
        }
        MaterialLookAndFeel.changeTheme(MaterialOceanicTheme())
        SwingUtilities.updateComponentTreeUI(owner)
    }
}
