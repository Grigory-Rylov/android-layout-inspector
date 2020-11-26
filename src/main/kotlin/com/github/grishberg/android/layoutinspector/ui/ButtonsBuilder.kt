package com.github.grishberg.android.layoutinspector.ui

import com.github.grishberg.android.layoutinspector.ui.layout.LayoutPanel
import com.github.grishberg.android.layoutinspector.ui.theme.Themes
import com.github.grishberg.android.layoutinspector.ui.tree.IconsStore
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.JButton
import javax.swing.JToolBar

private const val BUTTON_SIZE = 22

class ButtonsBuilder(
    private val layoutPanel: LayoutPanel,
    private val main: Main,
    private val themes: Themes
) : ActionListener {
    private val iconStore = IconsStore(BUTTON_SIZE - 8)

    fun addToolbarButtons(toolBar: JToolBar) {
        val resetZoomButton = makeToolbarButton(
            "1:1", "resetzoom",
            Actions.RESET_ZOOM,
            "Reset zoom (z)"
        )
        toolBar.add(resetZoomButton)

        val fitScreenButton = makeToolbarButton(
            "{-}", "fitscreen",
            Actions.FIT_TO_SCREEN,
            "Fits to screen (f)"
        )
        toolBar.add(fitScreenButton)

        val helpButton = makeToolbarButton(
            "?", "help",
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
            "/icons/dark/$iconName.svg"
        } else {
            "/icons/light/$iconName.svg"
        }
        val button = JButton(altText)
        button.actionCommand = actionCommand.name
        button.toolTipText = toolTipText
        button.addActionListener(this)
        button.preferredSize = Dimension(48, BUTTON_SIZE)
        button.maximumSize = Dimension(48, BUTTON_SIZE)
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
