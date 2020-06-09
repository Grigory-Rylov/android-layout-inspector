package com.github.grishberg.android.layoutinspector.ui.dialogs

import com.android.ddmlib.ClientsChangedListener
import com.android.ddmlib.ClientsListenerSetter
import com.android.ddmlib.IDevice
import com.android.layoutinspector.common.AppLogger
import com.android.layoutinspector.model.ClientWindow
import com.github.grishberg.android.layoutinspector.domain.LayoutRecordOptions
import com.github.grishberg.android.layoutinspector.domain.LayoutRecordOptionsInput
import com.github.grishberg.android.layoutinspector.process.providers.DeviceProvider
import com.github.grishberg.android.layoutinspector.settings.Settings
import com.github.grishberg.android.layoutinspector.ui.SETTINGS_ANDROID_HOME
import com.github.grishberg.android.layoutinspector.ui.common.LabeledGridBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import java.awt.Dimension
import java.awt.Frame
import java.awt.event.*
import java.text.NumberFormat
import java.util.concurrent.TimeUnit
import javax.swing.*
import javax.swing.text.NumberFormatter


private const val TAG = "NewLayoutDialog"
private const val TITLE = "Select Layout recording parameters"
private const val SETTINGS_TIMEOUT = "timeoutInSeconds"
private const val SETTINGS_ADB_INITIAL_REMOTE_ADDRESS = "remoteDeviceAddress"
private const val SETTINGS_WAIT_FOR_CLIENT_WINDOWS_TIMEOUT = "clientWindowsTimeout"

class NewLayoutDialog(
    private val owner: Frame,
    private val deviceProvider: DeviceProvider,
    private val logger: AppLogger,
    private val settings: Settings
) : CloseByEscapeDialog(owner, TITLE, true), LayoutRecordOptionsInput {
    private val clientWindowsTimeout = settings.getIntValueOrDefault(SETTINGS_WAIT_FOR_CLIENT_WINDOWS_TIMEOUT, 10)

    private val timeoutField: JFormattedTextField
    private val showAllPrecesses: JCheckBox
    private val clientListModel = DefaultListModel<ClientWrapper>()

    private val devicesModel = DevicesCompoBoxModel()
    private val devicesComboBox: JComboBox<DeviceWrapper>
    private val refreshButton: JButton
    private val startButton: JButton
    private val clientsList: JList<ClientWrapper>
    private val clientsChangedListener = CliensListener()
    private val deviceChangedAction = DeviceChangedActions()
    var result: LayoutRecordOptions? = null
        private set

    init {
        settings.setStringValue(SETTINGS_ADB_INITIAL_REMOTE_ADDRESS, "")
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
        devicesComboBox.model = devicesModel
        devicesComboBox.toolTipText = "Device selector"
        devicesComboBox.setPrototypeDisplayValue(EmptyDeviceWrapper("XXXXXXXXXXXXXXX"))

        devicesComboBox.addItemListener {
            if (it.stateChange != ItemEvent.SELECTED) {
                return@addItemListener
            }

            val device = it.item as DeviceWrapper
            ClientsListenerSetter.setClientsListener(device.device, clientsChangedListener)
            populateWithClients(device.device)
        }

        showAllPrecesses = JCheckBox("any processes")
        showAllPrecesses.addActionListener {
            val device = devicesComboBox.selectedItem as IDevice?

            if (device != null) {
                populateWithClients(device)
            }
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
        panelBuilder.addSingleComponent(showAllPrecesses)
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

        checkAndroidHome()
    }

    private fun checkAndroidHome() {
        if (settings.getStringValueOrDefault(SETTINGS_ANDROID_HOME, "").isEmpty()) {
            JOptionPane.showMessageDialog(
                owner,
                "For recording need to set ANDROID_HOME env variable." +
                        "\nIf value is already set, start app from terminal 'java -jar android-methods-profiler.jar'" +
                        "\nOr set '" + SETTINGS_ANDROID_HOME + "' in android-layout-inspector-settings.json"
            )
        }
    }

    private fun startRecording() {
        var currentDeviceIndex: Int = clientsList.selectedIndex
        if (currentDeviceIndex < 0) {
            logger.w("$TAG: startRecording() selectedIndex = $currentDeviceIndex")
            if (clientListModel.size() == 1) {
                currentDeviceIndex = 0
            } else {
                JOptionPane.showMessageDialog(
                    this, "Select device before starting", "Select device",
                    JOptionPane.ERROR_MESSAGE
                )
                return
            }

        }
        doStartRecording(currentDeviceIndex)
    }

    private fun doStartRecording(currentDeviceIndex: Int) {
        val client = clientListModel[currentDeviceIndex]
        val device = devicesComboBox.selectedItem as DeviceWrapper
        val timeoutInSeconds = timeoutField.text.toInt()
        result = LayoutRecordOptions(device.device, client.client, timeoutInSeconds)
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
        }
    }

    private fun populateWithClients(device: IDevice) {
        GlobalScope.launch(Dispatchers.Swing) {
            val clients = getClientsWithWindow(device, showAllPrecesses.isSelected)
            clientListModel.clear()
            for (c in clients) {
                clientListModel.addElement(c)
            }
            pack()
            repaint()

        }
    }

    private suspend fun getClientsWithWindow(device: IDevice, allProcesses: Boolean): List<ClientWrapper> {
        val async = GlobalScope.async {
            val clientsWithWindow = mutableListOf<ClientWrapper>()
            val clients = device.clients
            for (c in clients) {
                if (allProcesses) {
                    val element = ClientWrapper(c)
                    logger.d("$TAG: found client: $element")
                    clientsWithWindow.add(element)
                    continue
                }

                val windows =
                    ClientWindow.getAll(logger, c, clientWindowsTimeout.toLong(), TimeUnit.SECONDS) ?: emptyList()

                if (windows.isNotEmpty()) {
                    val element = ClientWrapper(c)
                    logger.d("$TAG: found client: $element, windows count: ${windows.size}")
                    clientsWithWindow.add(element)
                }
            }
            return@async clientsWithWindow
        }
        return async.await()
    }

    private inner class CliensListener : ClientsChangedListener {
        override fun onClientsChanged(device: IDevice) {
            if (!isVisible) {
                return
            }
            if (device != (devicesComboBox.selectedItem as DeviceWrapper).device) {
                return
            }
            SwingUtilities.invokeLater {
                populateWithClients(device)
            }
        }
    }

    private inner class DeviceChangedActions : DeviceProvider.DeviceChangedAction {
        override fun deviceConnected(device: IDevice) {
            if (!devicesModel.contains(device)) {
                devicesComboBox.addItem(RealDeviceWrapper(device))
                if (devicesModel.size == 1) {
                    devicesComboBox.selectedItem = device
                }
            }
        }

        override fun deviceDisconnected(device: IDevice) {
            devicesComboBox.removeItem(device)
            if (devicesModel.size == 0) {
                devicesComboBox.selectedItem = null
            }
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
