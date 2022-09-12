package com.github.grishberg.android.layoutinspector.ui.dialogs

import com.android.layoutinspector.common.AppLogger
import com.android.layoutinspector.model.ClientWindow
import com.github.grishberg.android.layoutinspector.domain.WindowsListInput
import com.github.grishberg.android.layoutinspector.settings.SettingsFacade
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Frame
import java.awt.event.ActionEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.AbstractAction
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.KeyStroke
import javax.swing.ListSelectionModel

private const val TITLE = "Select window"
private const val TAG = "WindowsDialog"

class WindowsDialog(
    private val owner: Frame,
    private val settings: SettingsFacade,
    private val logger: AppLogger
) : CloseByEscapeDialog(owner, TITLE, true), WindowsListInput {
    private val clientWindowList: JList<ClientWindow>

    private val clientWindowListModel = DefaultListModel<ClientWindow>()
    private val startButton = JButton("Start")

    init {
        clientWindowList = JBList(clientWindowListModel)
        clientWindowList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        val listScroll = JBScrollPane(clientWindowList)
        clientWindowList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(evt: MouseEvent) {
                if (evt.clickCount == 2) { // Double-click detected
                    isVisible = false
                }
            }
        })

        clientWindowList.getInputMap(JBList.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ENTER"), "start")
        clientWindowList.actionMap.put("start", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                if (clientWindowList.selectedIndex >= 0) {
                    isVisible = false
                }
            }
        })

        clientWindowList.addListSelectionListener {
            if (clientWindowList.selectedIndex >= 0) {
                startButton.isEnabled = true
            }
        }

        startButton.addActionListener {
            isVisible = false
        }

        listScroll.preferredSize = Dimension(640, 400)
        val content = JPanel()
        content.layout = BorderLayout()
        content.add(JLabel("Windows:"), BorderLayout.NORTH)
        content.add(listScroll, BorderLayout.CENTER)
        content.add(startButton, BorderLayout.SOUTH)
        startButton.isEnabled = false
        contentPane = content
        pack()
    }

    override suspend fun getSelectedWindow(windows: List<ClientWindow>): ClientWindow {
        logger.d("$TAG show dialog for client $windows")

        clientWindowListModel.clear()

        var selectedIndex = -1
        for (index in windows.indices) {
            val window = windows[index]
            clientWindowListModel.addElement(window)
            if (window.displayName == settings.lastWindowName) {
                selectedIndex = index
            }
            logger.d("$TAG found window $window")
        }
        setLocationRelativeTo(owner)
        startButton.isEnabled = false
        if (selectedIndex >= 0) {
            clientWindowList.selectedIndex = selectedIndex
        }
        isVisible = true

        logger.d("$TAG dialog is closed")
        val selectedWindow = clientWindowListModel[clientWindowList.selectedIndex]
        settings.lastWindowName = selectedWindow?.displayName ?: ""
        return selectedWindow
    }
}
