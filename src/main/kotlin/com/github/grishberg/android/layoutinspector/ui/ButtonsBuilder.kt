package com.github.grishberg.android.layoutinspector.ui

import com.github.grishberg.android.layoutinspector.ui.layout.LayoutPanel
import com.github.grishberg.android.layoutinspector.ui.theme.Themes
import com.github.grishberg.android.layoutinspector.ui.tree.IconsStore
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.JButton
import javax.swing.JToolBar
private const val BUTTON_SIZE = 32

class ButtonsBuilder(
    private val layoutPanel: LayoutPanel,
    private val main: Main,
    private val themes: Themes
) : ActionListener {
    private val iconStore = IconsStore()

    fun addToolbarButtons(toolBar: JToolBar) {
        val resetZoomButton = makeToolbarButton(
            "1:1", "resetzoom",
            Actions.RESET_ZOOM,
            "Reset zoom"
        )
        toolBar.add(resetZoomButton)

        val fitScreenButton = makeToolbarButton(
            "fit to screen", "fitscreen",
            Actions.FIT_TO_SCREEN,
            "Fits to screen"
        )
        toolBar.add(fitScreenButton)

        val helpButton = makeToolbarButton(
            "Manual", "help",
            Actions.HELP,
            "Go to home page"
        )
        toolBar.add(helpButton)
    }

    private fun makeToolbarButton(
        altText: String,
        iconName: String,
        actionCommand: Actions,
        toolTipText: String
    ): JButton? {
        val imageLocation = if (themes.isDark) {
            "/icons/dark/$iconName.png"
        } else {
            "/icons/light/$iconName.png"
        }
        val icon = iconStore.createImageIcon(imageLocation, altText)
        val button = JButton(icon)
        button.actionCommand = actionCommand.name
        button.toolTipText = toolTipText
        button.addActionListener(this)
        button.preferredSize = Dimension(BUTTON_SIZE, BUTTON_SIZE)
        button.maximumSize = Dimension(BUTTON_SIZE, BUTTON_SIZE)
        return button
    }

    override fun actionPerformed(e: ActionEvent) {
        if (e.actionCommand == Actions.RESET_ZOOM.name) {
            layoutPanel.resetZoom()
            return
        }

        if (e.actionCommand == Actions.FIT_TO_SCREEN.name) {
            layoutPanel.fitZoom()
            return
        }

        if (e.actionCommand == Actions.HELP.name) {
            main.goToHelp()
            return
        }
    }
}
