package com.github.grishberg.android.layoutinspector.ui.dialogs

import com.android.layoutinspector.common.AppLogger
import com.android.layoutinspector.model.ClientWindow
import com.github.grishberg.android.layoutinspector.domain.WindowsListInput
import com.github.grishberg.android.layoutinspector.ui.common.LabeledGridBuilder
import java.awt.Dimension
import java.awt.Frame
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JScrollPane
import javax.swing.ListSelectionModel

private const val TITLE = "Select window"
private const val TAG = "WindowsDialog"

class WindowsDialog(
    private val owner: Frame,
    private val logger: AppLogger
) : JDialog(owner, TITLE, true), WindowsListInput {
    private val clientWindowList: JList<ClientWindow>

    private val clientWindowListModel = DefaultListModel<ClientWindow>()
    private val startButton: JButton

    init {
        clientWindowList = JList(clientWindowListModel)
        clientWindowList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        val listScroll = JScrollPane(clientWindowList)
        clientWindowList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(evt: MouseEvent) {
                val list = evt.getSource() as JList<*>
                if (evt.clickCount == 2) { // Double-click detected
                    isVisible = false
                }
            }
        })
        startButton = JButton("Start")
        startButton.addActionListener {
            isVisible = false
        }

        listScroll.preferredSize = Dimension(640, 400)
        val panelBuilder = LabeledGridBuilder()
        panelBuilder.addSingleComponent(JLabel("Windows:"))
        panelBuilder.addSingleComponent(listScroll)
        panelBuilder.addSingleComponent(startButton)
        contentPane = panelBuilder.content
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
        isVisible = true


        logger.d("$TAG dialog is closed")
        return clientWindowListModel[clientWindowList.selectedIndex]
    }
}
