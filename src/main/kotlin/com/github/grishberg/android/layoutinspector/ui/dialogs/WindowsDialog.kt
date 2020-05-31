package com.github.grishberg.android.layoutinspector.ui.dialogs

import com.android.layoutinspector.common.AppLogger
import com.android.layoutinspector.model.ClientWindow
import com.github.grishberg.android.layoutinspector.domain.ClientWindowsInput
import com.github.grishberg.android.layoutinspector.domain.LayoutRecordOptions
import com.github.grishberg.android.layoutinspector.ui.common.LabeledGridBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import java.awt.Frame
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.concurrent.TimeUnit
import javax.swing.*

private const val TITLE = "Select window"
private const val TAG = "WindowsDialog"

class WindowsDialog(
    owner: Frame,
    private val logger: AppLogger
) : JDialog(owner, TITLE, true), ClientWindowsInput {
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

        val panelBuilder = LabeledGridBuilder()
        panelBuilder.addSingleComponent(JLabel("Windows:"))
        panelBuilder.addSingleComponent(listScroll)
        panelBuilder.addSingleComponent(startButton)
        contentPane = panelBuilder.content
        pack()
    }

    override suspend fun getSelectedWindow(options: LayoutRecordOptions): ClientWindow {
        logger.d("$TAG show dialog for client ${options.client}")

        clientWindowListModel.clear()
        val windows = GlobalScope.async(Dispatchers.IO) {
            val windows =
                ClientWindow.getAll(options.client, options.timeoutInSeconds.toLong(), TimeUnit.SECONDS) ?: emptyList()
            return@async windows
        }
        val windowList = windows.await()
        if (windowList.size == 1) {
            return windowList[0]
        }

        if (windowList.isEmpty()) {
            throw IllegalStateException("No windows for client")
        }

        for (w in windowList) {
            clientWindowListModel.addElement(w)
            logger.d("$TAG found window $w")
        }
        isVisible = true


        logger.d("$TAG dialog is closed")
        return clientWindowListModel[clientWindowList.selectedIndex]
    }
}