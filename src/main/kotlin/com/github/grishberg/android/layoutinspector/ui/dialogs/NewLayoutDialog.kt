package com.github.grishberg.android.layoutinspector.ui.dialogs

import com.android.ddmlib.IDevice
import com.android.layoutinspector.common.AppLogger
import com.android.layoutinspector.model.ClientWindow
import com.github.grishberg.android.layoutinspector.domain.LayoutRecordOptions
import com.github.grishberg.android.layoutinspector.domain.LayoutRecordOptionsInput
import com.github.grishberg.android.layoutinspector.process.providers.DeviceProvider
import com.github.grishberg.android.layoutinspector.settings.SettingsFacade
import com.github.grishberg.android.layoutinspector.ui.common.JNumberField
import com.github.grishberg.android.layoutinspector.ui.common.LabeledGridBuilder
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import java.awt.FocusTraversalPolicy
import java.awt.event.ActionEvent
import java.awt.event.ComponentEvent
import java.awt.event.ComponentListener
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.ItemEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.concurrent.TimeUnit
import javax.swing.AbstractAction
import javax.swing.BorderFactory
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JTextField
import javax.swing.KeyStroke
import javax.swing.ListSelectionModel
import javax.swing.UIManager


private const val TAG = "NewLayoutDialog"
private const val TITLE = "Select Layout recording parameters"

class NewLayoutDialog(
    private val owner: JFrame,
    private val deviceProvider: DeviceProvider,
    private val logger: AppLogger,
    private val settings: SettingsFacade
) : CloseByEscapeDialog(owner, TITLE, true), LayoutRecordOptionsInput {
    private val timeoutField = JNumberField(20)
    private val filePrefixField = JTextField(20)
    private val showAllProcesses: JCheckBox
    private val secondProtocolVersion: JCheckBox
    private val clientListModel = DefaultListModel<ClientWrapper>()

    private val devicesModel = DevicesCompoBoxModel()
    private val devicesComboBox: ComboBox<DeviceWrapper>
    private val startButton: JButton
    private val resetConnectionButton: JButton
    private val clientsList: JBList<ClientWrapper>
    private val deviceChangedAction = DeviceChangedActions()
    var result: LayoutRecordOptions? = null
        private set

    init {
        timeoutField.value = settings.captureLayoutTimeout.toInt()
        timeoutField.addActionListener {
            startRecording()
        }

        devicesComboBox = ComboBox()
        devicesComboBox.model = devicesModel
        devicesComboBox.toolTipText = "Device selector"
        devicesComboBox.prototypeDisplayValue = EmptyDeviceWrapper("XXXXXXXXXXXXXXX")

        devicesComboBox.addItemListener {
            if (it.stateChange != ItemEvent.SELECTED) {
                return@addItemListener
            }

            val device = it.item as DeviceWrapper
            populateWithClients(device.device)
        }

        showAllProcesses = JCheckBox("any processes")
        showAllProcesses.addActionListener {
            val deviceWrapper = devicesComboBox.selectedItem as DeviceWrapper?

            if (deviceWrapper != null) {
                populateWithClients(deviceWrapper.device)
            }
        }

        clientsList = JBList(clientListModel)
        val listScroll = JBScrollPane(clientsList)
        setupClientsList(clientsList)

        listScroll.preferredSize = Dimension(300, 400)

        filePrefixField.toolTipText = "If not empty - will adds prefix to file name."
        filePrefixField.text = settings.fileNamePrefix

        secondProtocolVersion = JCheckBox("protocol ver. 2")
        secondProtocolVersion.toolTipText = "if not selected, will be used ver. 1, which slower, but has more properties"
        secondProtocolVersion.isSelected = settings.isSecondProtocolVersionEnabled

        startButton = JButton("Start")
        startButton.addActionListener {
            startRecording()
        }

        resetConnectionButton = JButton("Reset ADB")
        resetConnectionButton.addActionListener {
            resetAdbConnection()
        }

        val panelBuilder = LabeledGridBuilder()
        panelBuilder.addLabeledComponent("device: ", devicesComboBox)
        panelBuilder.addSingleComponent(JLabel("Applications:"))
        panelBuilder.addSingleComponent(listScroll)
        panelBuilder.addSingleComponent(showAllProcesses)
        panelBuilder.addLabeledComponent("timeout in seconds: ", timeoutField)
        panelBuilder.addLabeledComponent("File name prefix: ", filePrefixField)
        panelBuilder.addSingleComponent(secondProtocolVersion)
        if (deviceProvider.isReconnectionAllowed) {
            panelBuilder.addMainAndSlaveComponent(startButton, resetConnectionButton)
        } else {
            panelBuilder.addSingleComponent(startButton)
        }
        contentPane = panelBuilder.content

        contentPane.focusTraversalPolicy = FocusPolicy()

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

    private fun setupClientsList(clientsList: JBList<ClientWrapper>) {
        clientsList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        clientsList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(evt: MouseEvent) {
                val list = evt.getSource() as JBList<*>
                if (evt.clickCount == 2) { // Double-click detected
                    val index = list.locationToIndex(evt.point)
                    startRecording()
                }
            }
        })
        clientsList.getInputMap(JBList.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ENTER"), "start")
        clientsList.actionMap.put("start", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                if (this@NewLayoutDialog.clientsList.model.size == 0) {
                    return
                }
                startRecording()
            }
        })
        clientsList.addFocusListener(object : FocusListener {
            override fun focusGained(e: FocusEvent?) {
                clientsList.border = BorderFactory.createLineBorder(UIManager.getColor("Button.focus"))
                if (clientsList.itemsCount == 0) {
                    return
                }
                val selected = clientsList.selectedIndex
                if (selected < 0) {
                    clientsList.selectedIndex = 0
                }
            }

            override fun focusLost(e: FocusEvent?) {
                clientsList.border = BorderFactory.createEmptyBorder()
            }
        })
    }

    override fun onDialogClosed() {
        if (settings.shouldStopAdbAfterJob()) {
            deviceProvider.stop()
        }
    }

    private fun resetAdbConnection() {
        startButton.isEnabled = false
        clientListModel.clear()
        devicesComboBox.removeAll()
        deviceProvider.reconnect()
    }

    private fun startRecording() {
        settings.fileNamePrefix = filePrefixField.text.trim()
        settings.captureLayoutTimeout = timeoutField.value.toLong()
        var currentClientIndex: Int = clientsList.selectedIndex
        if (currentClientIndex < 0) {
            logger.w("$TAG: startRecording() currentClientIndex = $currentClientIndex")
            if (clientListModel.size() == 1) {
                currentClientIndex = 0
            } else {
                JOptionPane.showMessageDialog(
                    this, "Select application before starting", "Select application",
                    JOptionPane.ERROR_MESSAGE
                )
                return
            }

        }
        doStartRecording(currentClientIndex)
    }

    private fun doStartRecording(currentClientIndex: Int) {
        val client = clientListModel[currentClientIndex]
        settings.lastProcessName = client.toString()
        val device = devicesComboBox.selectedItem as DeviceWrapper
        val timeoutInSeconds = timeoutField.text.toInt()
        result = LayoutRecordOptions(device.device, client.client, timeoutInSeconds, filePrefixField.text.trim(), secondProtocolVersion.isSelected)
        deviceProvider.deviceChangedActions.remove(deviceChangedAction)
        isVisible = false
    }


    override suspend fun getLayoutOptions(): LayoutRecordOptions? {
        showDialog()

        return result
    }

    fun showDialog() {
        deviceProvider.deviceChangedActions.add(deviceChangedAction)
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
                devicesComboBox.addItem(RealDeviceWrapper(device))
            }

            if (devicesComboBox.itemCount > 0) {
                devicesComboBox.selectedIndex = 0
            }
            if (devicesComboBox.itemCount == 1) {
                clientsList.requestFocus()
            }
        }
    }

    private fun populateWithClients(device: IDevice) {
        GlobalScope.launch(Dispatchers.Swing) {
            val clients = getClientsWithWindow(device, showAllProcesses.isSelected)
            clientListModel.clear()
            var selectedIndex = 0
            for (i in clients.indices) {
                val client = clients[i]
                clientListModel.addElement(client)
                if (client.toString() == settings.lastProcessName) {
                    selectedIndex = i
                }
            }
            startButton.isEnabled = !clientListModel.isEmpty
            if (!clientListModel.isEmpty && clientsList.selectedIndex < 0) {
                clientsList.selectedIndex = selectedIndex
            }
            pack()
            repaint()
        }
    }

    private suspend fun getClientsWithWindow(device: IDevice, allProcesses: Boolean): List<ClientWrapper> {

        val errorHandler = CoroutineExceptionHandler { _, exception ->
            logger.e("getClientsWithWindow error", exception)
        }
        val async = GlobalScope.async(errorHandler) {
            val clientsWithWindow = mutableListOf<ClientWrapper>()
            val clients = device.clients
            for (c in clients) {
                if (allProcesses) {
                    val element = ClientWrapper(c)
                    logger.d("$TAG: found client: $element")
                    clientsWithWindow.add(element)
                    continue
                }

                try {
                    val windows =
                        ClientWindow.getAll(logger, c, settings.clientWindowsTimeout, TimeUnit.SECONDS) ?: emptyList()

                    if (windows.isNotEmpty()) {
                        val element = ClientWrapper(c)
                        logger.d("$TAG: found client: $element, windows count: ${windows.size}")
                        clientsWithWindow.add(element)
                    }
                } catch (e: Exception) {
                    logger.e("ClientWindow.getAll error", e)
                }
            }
            return@async clientsWithWindow
        }
        return async.await()
    }


    private inner class DeviceChangedActions : DeviceProvider.DeviceChangedAction {
        override fun deviceConnected(device: IDevice) {
            if (!devicesModel.contains(device)) {
                devicesComboBox.addItem(RealDeviceWrapper(device))
                if (devicesModel.size == 1) {
                    devicesComboBox.selectedItem = device
                    devicesComboBox.selectedIndex = 0
                }
            } else {
                val selectedDeviceWrapper = devicesComboBox.selectedItem as DeviceWrapper
                populateWithClients(selectedDeviceWrapper.device)
            }
        }

        override fun deviceDisconnected(device: IDevice) {
            devicesComboBox.removeItem(RealDeviceWrapper(device))
            if (devicesModel.size == 0) {
                devicesComboBox.selectedItem = null
            }
        }
    }

    private inner class FocusPolicy : FocusTraversalPolicy() {
        override fun getComponentAfter(aContainer: Container, aComponent: Component): Component {
            return when (aComponent) {
                devicesComboBox -> clientsList
                clientsList -> showAllProcesses
                showAllProcesses -> timeoutField
                timeoutField -> filePrefixField
                filePrefixField -> startButton
                else -> getFirstComponent(aContainer)
            }
        }

        override fun getComponentBefore(aContainer: Container, aComponent: Component): Component {// not used
            return when (aComponent) {
                devicesComboBox -> clientsList
                clientsList -> devicesComboBox
                showAllProcesses -> clientsList
                timeoutField -> showAllProcesses
                filePrefixField -> timeoutField
                else -> getLastComponent(aContainer)
            }
        }

        override fun getFirstComponent(aContainer: Container): Component {
            if (devicesComboBox.model.size == 1) {
                return clientsList
            }
            return devicesComboBox
        }

        override fun getLastComponent(aContainer: Container): Component {
            return startButton
        }

        override fun getDefaultComponent(aContainer: Container): Component {
            return clientsList
        }
    }
}

class EmptyDeviceWrapper(private val name: String) : DeviceWrapper {
    override val device: IDevice
        get() = throw IllegalStateException("Stub")

    override fun toString(): String {
        return name
    }
}
