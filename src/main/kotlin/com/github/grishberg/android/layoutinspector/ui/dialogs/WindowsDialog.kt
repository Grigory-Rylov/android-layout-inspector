package com.github.grishberg.android.layoutinspector.ui.dialogs

import com.android.layoutinspector.common.AppLogger
import com.android.layoutinspector.model.ClientWindow
import com.github.grishberg.android.layoutinspector.domain.WindowsListInput
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Frame
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListSelectionModel

private const val TITLE = "Select window"
private const val TAG = "WindowsDialog"

class WindowsDialog(
    private val owner: Frame,
    private val logger: AppLogger
) : JDialog(owner, TITLE, true), WindowsListInput {
    private val clientWindowList: JList<ClientWindow>

    private val clientWindowListModel = DefaultListModel<ClientWindow>()
    private val startButton = JButton("Start")

    init {
        clientWindowList = JBList(clientWindowListModel)
        clientWindowList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        val listScroll = JBScrollPane(clientWindowList)
        clientWindowList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(evt: MouseEvent) {
                val list = evt.getSource() as JList<*>
                if (evt.clickCount == 2) { // Double-click detected
                    isVisible = false
                }
                if (clientWindowList.selectedIndex >= 0) {
                    startButton.isEnabled = true
                }
            }
        })

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

        for (w in windows) {
            clientWindowListModel.addElement(w)
            logger.d("$TAG found window $w")
        }
        setLocationRelativeTo(owner)
        startButton.isEnabled = false
        isVisible = true


        logger.d("$TAG dialog is closed")
        return clientWindowListModel[clientWindowList.selectedIndex]
    }
}
