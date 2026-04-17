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
import com.intellij.openapi.application.EDT
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import kotlinx.coroutines.*
import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import java.awt.FocusTraversalPolicy
import java.awt.event.*
import java.util.concurrent.TimeUnit
import javax.swing.*


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
    private val dumpViewMode: JCheckBox
    private val clientListModel = DefaultListModel<ClientWrapper>()

    private val devicesModel = DevicesCompoBoxModel()
    private val devicesComboBox: ComboBox<DeviceWrapper>
    private val startButton: JButton
    private val resetConnectionButton: JButton
    private val clientsList: JBList<ClientWrapper>
    private val deviceChangedAction = DeviceChangedActions()
    private var processesRefreshJob: Job? = null
    var result: LayoutRecordOptions? = null
        private set

    init {
        timeoutField.value = settings.captureLayoutTimeout.toInt()
        timeoutField.addActionListener {
            startRecording()
        }

        filePrefixField.addActionListener {
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
        showAllProcesses.isSelected = true
        showAllProcesses.addActionListener {
            val deviceWrapper = devicesComboBox.selectedItem as DeviceWrapper?

            if (deviceWrapper != null) {
                populateWithClients(deviceWrapper.device)
            }
        }

        // Start processes refresh timer when dialog is shown
        addWindowListener(object : java.awt.event.WindowAdapter() {
            override fun windowOpened(e: java.awt.event.WindowEvent?) {
                logger.d("$TAG: windowOpened() - Starting process auto-refresh")
                // Immediately populate clients for the selected device
                val deviceWrapper = devicesComboBox.selectedItem as DeviceWrapper?
                if (deviceWrapper != null) {
                    logger.d("$TAG: windowOpened() - Populating clients for device: ${deviceWrapper.device}")
                    populateWithClients(deviceWrapper.device)
                } else {
                    logger.w("$TAG: windowOpened() - No device selected")
                }
                startProcessesRefreshTimer()
            }

            override fun windowClosed(e: java.awt.event.WindowEvent?) {
                logger.d("$TAG: windowClosed() - Stopping process auto-refresh")
                stopProcessesRefreshTimer()
            }
        })

        clientsList = JBList(clientListModel)
        val listScroll = JBScrollPane(clientsList)
        setupClientsList(clientsList)

        listScroll.preferredSize = Dimension(300, 400)

        filePrefixField.toolTipText = "If not empty - will adds prefix to file name."
        filePrefixField.text = settings.fileNamePrefix

        secondProtocolVersion = JCheckBox("protocol ver. 2")
        secondProtocolVersion.toolTipText =
            "if not selected, will be used ver. 1, which slower, but has more properties"
        secondProtocolVersion.isSelected = settings.isSecondProtocolVersionEnabled

        dumpViewMode = JCheckBox("uiautomator dump")
        dumpViewMode.toolTipText = "if selected, will be used layouts from uiautomator dump for ComposeView children"
        dumpViewMode.isSelected = settings.isDumpViewModeEnabled

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
        panelBuilder.addSingleComponent(dumpViewMode)
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
        settings.isDumpViewModeEnabled = dumpViewMode.isSelected
        settings.isSecondProtocolVersionEnabled = secondProtocolVersion.isSelected
        result = LayoutRecordOptions(
            device = device.device,
            client = client.client,
            timeoutInSeconds = timeoutInSeconds,
            fileNamePrefix = filePrefixField.text.trim(),
            v2Enabled = secondProtocolVersion.isSelected,
            dumpViewModeEnabled = dumpViewMode.isSelected,
            label = filePrefixField.text
        )
        deviceProvider.deviceChangedActions.remove(deviceChangedAction)
        isVisible = false
    }

    override suspend fun getLayoutOptions(): LayoutRecordOptions? {
        showDialog()

        return result
    }

    fun showDialog() {
        logger.d("$TAG: showDialog() - Opening dialog")
        deviceProvider.deviceChangedActions.add(deviceChangedAction)
        setLocationRelativeTo(owner)
        result = null
        populateWithDevices()
        isVisible = true
        logger.d("$TAG: showDialog() - Dialog closed, result: ${result != null}")
    }

    private fun populateWithDevices() {
        logger.d("$TAG: populateWithDevices() - Requesting devices from provider")
        GlobalScope.launch(Dispatchers.EDT) {
            val devices = deviceProvider.requestDevices()
            logger.d("$TAG: populateWithDevices() - received ${devices.size} devices")
            devicesComboBox.removeAllItems()
            for (device in devices) {
                devicesComboBox.addItem(RealDeviceWrapper(device))
                logger.d("$TAG: populateWithDevices() - added device: ${device.serial}")
            }

            if (devicesComboBox.itemCount > 0) {
                devicesComboBox.selectedIndex = 0
                // Immediately populate clients for the first device
                logger.d("$TAG: populateWithDevices() - selecting first device and populating clients")
                populateWithClients((devicesComboBox.selectedItem as DeviceWrapper).device)
            }
            if (devicesComboBox.itemCount == 1) {
                clientsList.requestFocus()
            }
        }
    }

    private fun populateWithClients(device: IDevice) {
        logger.d("$TAG: populateWithClients() called for device: ${device.serial}")
        GlobalScope.launch(Dispatchers.EDT) {
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
            logger.d("$TAG: populateWithClients() completed, clients count: ${clients.size}")
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
                        ClientWindow.getAllV2(logger, c, settings.clientWindowsTimeout, TimeUnit.SECONDS) ?: emptyList()

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

    private fun startProcessesRefreshTimer() {
        logger.d("$TAG: startProcessesRefreshTimer() - Starting 3-second refresh timer")
        processesRefreshJob = GlobalScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(3000) // 3 seconds
                withContext(Dispatchers.EDT) {
                    val deviceWrapper = devicesComboBox.selectedItem as DeviceWrapper?
                    if (deviceWrapper != null) {
                        logger.d("$TAG: timer tick - populating clients for device: ${deviceWrapper.device.serial}")
                        populateWithClients(deviceWrapper.device)
                    } else {
                        logger.w("$TAG: timer tick - no device selected, skipping refresh")
                    }
                }
            }
        }
        logger.d("$TAG: startProcessesRefreshTimer() - Timer job started: ${processesRefreshJob?.hashCode()}")
    }

    private fun stopProcessesRefreshTimer() {
        logger.d("$TAG: stopProcessesRefreshTimer() - Cancelling timer job: ${processesRefreshJob?.hashCode()}")
        processesRefreshJob?.cancel()
        processesRefreshJob = null
        logger.d("$TAG: stopProcessesRefreshTimer() - Timer stopped")
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
