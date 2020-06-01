package com.github.grishberg.android.layoutinspector.ui.dialogs

import com.android.ddmlib.ClientsChangedListener
import com.android.ddmlib.ClientsListenerSetter
import com.android.ddmlib.IDevice
import com.android.layoutinspector.common.AppLogger
import com.android.layoutinspector.settings.Settings
import com.github.grishberg.android.layoutinspector.domain.LayoutRecordOptions
import com.github.grishberg.android.layoutinspector.domain.LayoutRecordOptionsInput
import com.github.grishberg.android.layoutinspector.process.providers.ClientsProvider
import com.github.grishberg.android.layoutinspector.process.providers.DeviceProvider
import com.github.grishberg.android.layoutinspector.ui.common.LabeledGridBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import java.awt.Dimension
import java.awt.Frame
import java.awt.event.*
import java.text.NumberFormat
import javax.swing.*
import javax.swing.text.NumberFormatter


private const val TAG = "NewLayoutDialog"
private const val TITLE = "Select Layout recording parameters"
private const val SETTINGS_PKG = "package"
private const val SETTINGS_TIMEOUT = "timeoutInSeconds"
private const val SETTINGS_ADB_INITIAL_COMMAND = "adbInitialCommand"

class NewLayoutDialog(
    private val owner: Frame,
    private val deviceProvider: DeviceProvider,
    private val clientsProvider: ClientsProvider,
    private val logger: AppLogger,
    private val settings: Settings
) : JDialog(owner, TITLE, true), LayoutRecordOptionsInput {
    private val timeoutField: JFormattedTextField
    private val devicesComboBox: JComboBox<IDevice>
    private val refreshButton: JButton
    private val startButton: JButton
    private val clientsList: JList<ClientWrapper>
    private val clientListModel = DefaultListModel<ClientWrapper>()
    private val clientsChangedListener = CliensListener()
    var result: LayoutRecordOptions? = null
        private set

    init {
        settings.setStringValue(SETTINGS_ADB_INITIAL_COMMAND, "")
        val format = NumberFormat.getInstance()
        val formatter = NumberFormatter(format)
        formatter.valueClass = Integer::class.java
        formatter.minimum = 1
        formatter.maximum = Int.MAX_VALUE
        formatter.allowsInvalid = false
        formatter.commitsOnValidEdit = true
        timeoutField = JFormattedTextField(formatter)
        timeoutField.value = settings.getIntValueOrDefault(SETTINGS_TIMEOUT, 60)
        timeoutField.addActionListener {
            startRecording()
        }

        devicesComboBox = JComboBox()
        devicesComboBox.setToolTipText("Device selector")
        devicesComboBox.addItemListener {
            if (it.stateChange != ItemEvent.SELECTED) {
                return@addItemListener
            }

            val device = it.item as IDevice
            ClientsListenerSetter.setClientsListener(device, clientsChangedListener)
            populateWithClients(device)
        }

        clientsList = JList(clientListModel)
        clientsList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        val listScroll = JScrollPane(clientsList)
        clientsList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(evt: MouseEvent) {
                val list = evt.getSource() as JList<*>
                if (evt.clickCount == 2) { // Double-click detected
                    val index = list.locationToIndex(evt.point)
                    startRecording()
                }
            }
        })
        listScroll.preferredSize = Dimension(300, 400)

        refreshButton = JButton("Refresh app list")
        startButton = JButton("Start")
        startButton.addActionListener {
            startRecording()
        }

        val panelBuilder = LabeledGridBuilder()
        panelBuilder.addLabeledComponent("device: ", devicesComboBox)
        panelBuilder.addSingleComponent(JLabel("Applications:"))
        panelBuilder.addSingleComponent(listScroll)
        panelBuilder.addLabeledComponent("timeout in seconds: ", timeoutField)
        panelBuilder.addSingleComponent(startButton)
        contentPane = panelBuilder.content
        pack()
        addComponentListener(object : ComponentListener {
            override fun componentResized(e: ComponentEvent?) {
                pack()
            }

            override fun componentMoved(e: ComponentEvent?) = Unit
            override fun componentHidden(e: ComponentEvent?) = Unit
            override fun componentShown(e: ComponentEvent?) = Unit
        })
    }

    private fun startRecording() {
        val client = clientListModel[clientsList.selectedIndex]
        val device = devicesComboBox.selectedItem as IDevice
        val timeoutInSeconds = timeoutField.text.toInt()
        result = LayoutRecordOptions(device, client.client, timeoutInSeconds)
        isVisible = false
    }


    override suspend fun getLayoutOptions(): LayoutRecordOptions? {
        showDialog()

        return result
    }

    fun showDialog() {
        setLocationRelativeTo(owner)
        result = null
        populateWithDevices()
        isVisible = true
    }

    private fun populateWithDevices() {
        GlobalScope.launch(Dispatchers.Swing) {
            val devices = deviceProvider.requestDevices()
            logger.d("$TAG: received ${devices.size} devices")
            devicesComboBox.removeAllItems()
            for (device in devices) {
                devicesComboBox.addItem(device)
            }

            if (devicesComboBox.itemCount > 0) {
                devicesComboBox.selectedIndex = 0
            }
        }
    }

    private fun populateWithClients(device: IDevice) {
        clientListModel.clear()
        val clients = device.clients
        for (c in clients) {
            val element = ClientWrapper(c)
            logger.d("$TAG: found client: $element")
            clientListModel.addElement(element)
        }
        pack()
        repaint()
    }

    private inner class CliensListener : ClientsChangedListener {
        override fun onClientsChanged(device: IDevice) {
            if (!isVisible) {
                return
            }
            if (device != devicesComboBox.selectedItem as IDevice) {
                return
            }
            SwingUtilities.invokeLater {
                populateWithClients(device)
            }
        }
    }
}
