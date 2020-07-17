package com.github.grishberg.android.layoutinspector.ui

import com.android.ddmlib.DdmPreferences
import com.android.layoutinspector.common.AppLogger
import com.android.layoutinspector.common.SimpleConsoleLogger
import com.android.layoutinspector.model.LayoutFileData
import com.android.layoutinspector.model.ViewNode
import com.github.grishberg.android.layoutinspector.domain.DialogsInput
import com.github.grishberg.android.layoutinspector.domain.LayoutResultOutput
import com.github.grishberg.android.layoutinspector.domain.Logic
import com.github.grishberg.android.layoutinspector.domain.MetaRepository
import com.github.grishberg.android.layoutinspector.process.LayoutFileSystem
import com.github.grishberg.android.layoutinspector.process.providers.DeviceProvider
import com.github.grishberg.android.layoutinspector.settings.JsonSettings
import com.github.grishberg.android.layoutinspector.settings.Settings
import com.github.grishberg.android.layoutinspector.settings.SettingsFacade
import com.github.grishberg.android.layoutinspector.ui.dialogs.FindDialog
import com.github.grishberg.android.layoutinspector.ui.dialogs.LoadingDialog
import com.github.grishberg.android.layoutinspector.ui.dialogs.NewLayoutDialog
import com.github.grishberg.android.layoutinspector.ui.dialogs.WindowsDialog
import com.github.grishberg.android.layoutinspector.ui.dialogs.bookmarks.Bookmarks
import com.github.grishberg.android.layoutinspector.ui.info.PropertiesPanel
import com.github.grishberg.android.layoutinspector.ui.layout.DistanceType
import com.github.grishberg.android.layoutinspector.ui.layout.LayoutLogic
import com.github.grishberg.android.layoutinspector.ui.layout.LayoutPanel
import com.github.grishberg.android.layoutinspector.ui.theme.ThemeProxy
import com.github.grishberg.android.layoutinspector.ui.tree.TreePanel
import com.github.grishberg.tracerecorder.adb.AdbWrapper
import com.github.grishberg.tracerecorder.adb.AdbWrapperImpl
import com.github.grishberg.tracerecorder.exceptions.DebugPortBusyException
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.io.File
import java.net.Socket
import java.net.SocketException
import javax.swing.*
import javax.swing.BoxLayout
import javax.swing.border.BevelBorder
import javax.swing.filechooser.FileNameExtensionFilter


private const val INITIAL_SCREEN_WIDTH = 1024
private const val INITIAL_SCREEN_HEIGHT = 600
private const val INITIAL_LAYOUTS_WINDOW_WIDTH = 300
private const val INITIAL_PROPERTIES_WINDOW_WIDTH = 400

enum class OpenWindowMode {
    DEFAULT,
    OPEN_FILE
}

// create a class MainWindow extending JFrame
class Main(
    private val mode: OpenWindowMode,
    private val settings: Settings,
    private val logger: AppLogger
) : JFrame("Yet Another Android Layout Inspector. ver${javaClass.getPackage().implementationVersion}"),
    LayoutResultOutput, DialogsInput {

    // Declaration of objects of the
    // JScrollPane class.
    private val layoutPanel: LayoutPanel
    private val treePanel: TreePanel
    private val propertiesPanel: PropertiesPanel
    private val logic: Logic
    private val statusLabel: JLabel

    private val adb: AdbWrapper

    private val splitPane1: JSplitPane
    private val splitPane2: JSplitPane
    private val fileMenu: JMenu
    private val loadingDialog: LoadingDialog
    private val windowsDialog: WindowsDialog
    private val findDialog: FindDialog

    private val mainPanel: JPanel
    private val statusDistanceLabel: JLabel

    private val settingsFacade: SettingsFacade
    private val themeProxy = ThemeProxy()
    private val themes: Themes
    private val filter = FileNameExtensionFilter("Layout inspector files", "li")
    private val bookmarks = Bookmarks()
    private val metaRepository = MetaRepository(logger, bookmarks)

    // Constructor of MainWindow class
    init {
        settingsFacade = SettingsFacade(settings)

        themes = Themes(this, settingsFacade, themeProxy, logger)

        val androidHome = System.getenv("ANDROID_HOME")
        if (androidHome != null) {
            settingsFacade.androidHome = androidHome
        }

        defaultCloseOperation = JFrame.EXIT_ON_CLOSE

        layoutPanel = LayoutPanel(metaRepository, settingsFacade)
        treePanel = TreePanel(this, themeProxy, bookmarks, main = this)
        propertiesPanel = PropertiesPanel(metaRepository)
        propertiesPanel.setSizeDpMode(settingsFacade.shouldShowSizeInDp())
        layoutPanel.setSizeDpMode(settingsFacade.shouldShowSizeInDp())

        val selectionAction = TreeNodeSelectedAction()
        treePanel.nodeSelectedAction = selectionAction
        layoutPanel.setOnLayoutSelectedAction(selectionAction)
        findDialog = FindDialog(this)
        findDialog.foundAction = selectionAction

        val treeScrollPane = JScrollPane(
            treePanel,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS
        )

        mainPanel = JPanel(BorderLayout())
        splitPane1 = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, layoutPanel, treeScrollPane)
        splitPane2 = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, splitPane1, propertiesPanel.getComponent())
        splitPane2.resizeWeight = 1.0
        splitPane1.dividerLocation = INITIAL_LAYOUTS_WINDOW_WIDTH
        splitPane2.dividerLocation = INITIAL_SCREEN_WIDTH - (INITIAL_PROPERTIES_WINDOW_WIDTH)
        mainPanel.add(splitPane2, BorderLayout.CENTER)


        statusLabel = JLabel()
        mainPanel.add(statusLabel, BorderLayout.NORTH)

        statusDistanceLabel = JLabel()
        createStatusBar(statusLabel)

        contentPane = mainPanel

        fileMenu = createFileMenu()
        createMenu(fileMenu)

        pack()

        DdmPreferences.setSelectedDebugPort(settingsFacade.adbPort)

        windowsDialog = WindowsDialog(this, logger)

        adb = AdbWrapperImpl(true, InspectorLogger(), settingsFacade.androidHome)
        val deviceProvider = DeviceProvider(logger, adb, settings)

        val devicesInputDialog = NewLayoutDialog(this, deviceProvider, logger, settingsFacade)

        loadingDialog = LoadingDialog(this)
        val fileSystem = LayoutFileSystem(logger)
        logic = Logic(
            devicesInputDialog,
            windowsDialog,
            this,
            logger,
            fileSystem,
            this,
            metaRepository
        )

        Runtime.getRuntime().addShutdownHook(Thread(this::doOnClose))

        setSize(INITIAL_SCREEN_WIDTH, INITIAL_SCREEN_HEIGHT)
        defaultCloseOperation = DISPOSE_ON_CLOSE
        isLocationByPlatform = true
        isVisible = true
    }

    private fun createStatusBar(statusLabel: JLabel) {
        val statusPanel = JPanel()
        statusPanel.border = BevelBorder(BevelBorder.LOWERED)
        mainPanel.add(statusPanel, BorderLayout.SOUTH)
        val font = statusLabel.font
        statusPanel.preferredSize = Dimension(width, font.size + 4)
        statusPanel.layout = BoxLayout(statusPanel, BoxLayout.X_AXIS)
        this.statusLabel.horizontalAlignment = SwingConstants.LEFT
        statusPanel.add(statusLabel)
    }

    fun initUi() {
        KeyBinder(mainPanel, layoutPanel, logic, this)
        if (mode == OpenWindowMode.DEFAULT) {
            startRecording()
        } else {
            logic.openFile()
        }
    }

    private fun doOnClose() {
        settings.save()
        if (settingsFacade.shouldStopAdbAfterJob()) {
            adb.stop()
        }
    }

    private fun createMenu(fileMenu: JMenu) {
        val menuBar = JMenuBar()
        menuBar.add(fileMenu)
        menuBar.add(createViewMenu())
        menuBar.add(createSettingsMenu())
        menuBar.add(themes.createThemeMenu())
        jMenuBar = menuBar
    }

    private fun createFileMenu(): JMenu {
        val file = JMenu("File")

        val openFile = JMenuItem("Open")
        file.add(openFile)
        openFile.addActionListener { arg0: ActionEvent? -> openExistingFile() }

        val openFileInNewWindow = JMenuItem("Open in new window")
        file.add(openFileInNewWindow)
        openFileInNewWindow.addActionListener { arg0: ActionEvent? -> openExistingFile(newWindow = true) }

        val newFile = JMenuItem("Record new Layout info")
        file.add(newFile)
        newFile.addActionListener { arg0: ActionEvent? -> startRecording() }


        file.addSeparator()
        return file
    }

    private fun createViewMenu(): JMenu? {
        val viewMenu = JMenu("View")

        val pixelsMode = JRadioButtonMenuItem("Size in px")
        val dpMode = JRadioButtonMenuItem("Size in dp")

        val bg = ButtonGroup()
        bg.add(pixelsMode)
        bg.add(dpMode)

        viewMenu.add(pixelsMode)
        viewMenu.add(dpMode)

        if (settingsFacade.shouldShowSizeInDp()) {
            dpMode.isSelected = true
        } else {
            pixelsMode.isSelected = true
        }

        pixelsMode.addActionListener { e: ActionEvent ->
            setSizeDpMode(false)
            settingsFacade.showSizeInDp(false)
        }
        dpMode.addActionListener { e: ActionEvent ->
            setSizeDpMode(true)
            settingsFacade.showSizeInDp(true)
        }

        viewMenu.addSeparator()

        val openFind = JMenuItem("Find")
        viewMenu.add(openFind)
        openFind.addActionListener { arg0: ActionEvent? -> showFindDialog() }
        return viewMenu
    }

    private fun createSettingsMenu(): JMenu {
        val settingsMenu = JMenu("Settings")

        val disconnectAdbAfterJob = JCheckBoxMenuItem("Disconnect ADB after operation")
        disconnectAdbAfterJob.isSelected = settingsFacade.shouldStopAdbAfterJob()
        disconnectAdbAfterJob.addActionListener { e ->
            val aButton = e.source as AbstractButton
            settingsFacade.setStopAdbAfterJob(aButton.model.isSelected)
        }
        settingsMenu.add(disconnectAdbAfterJob)


        val allowSelectNotDrawnView = JCheckBoxMenuItem("Allow select hidden view")
        allowSelectNotDrawnView.isSelected = settingsFacade.allowedSelectHiddenView
        allowSelectNotDrawnView.addActionListener { e ->
            val aButton = e.source as AbstractButton
            settingsFacade.allowedSelectHiddenView = aButton.model.isSelected
        }
        settingsMenu.add(allowSelectNotDrawnView)
        return settingsMenu
    }

    private fun setSizeDpMode(enabled: Boolean) {
        statusLabel.text = ""
        settingsFacade.showSizeInDp(enabled)
        propertiesPanel.setSizeDpMode(enabled)
        layoutPanel.setSizeDpMode(enabled)
    }

    private fun startRecording() {
        if (isPortAlreadyUsed(DdmPreferences.getSelectedDebugPort())) {
            throw DebugPortBusyException(DdmPreferences.getSelectedDebugPort())
        }
        initBaseDebugPort()

        logic.startRecording()
    }

    fun openExistingFile(newWindow: Boolean = false) {
        if (newWindow) {
            val main = Main(OpenWindowMode.OPEN_FILE, settings, logger)
            main.initUi()
            return
        }
        logic.openFile()
    }

    override fun showResult(resultOutput: LayoutFileData) {
        layoutPanel.showLayoutResult(resultOutput)
        treePanel.showLayoutResult(resultOutput)
        findDialog.updateRootNode(resultOutput.node)
        splitPane1.invalidate()
    }

    override fun showError(error: String) {
        statusLabel.text = error
    }

    private inner class TreeNodeSelectedAction : TreePanel.OnNodeSelectedAction, LayoutLogic.OnLayoutSelectedAction,
        FindDialog.OnFoundAction {
        override fun onViewNodeSelected(node: ViewNode) {
            layoutPanel.selectNode(node)
            propertiesPanel.showProperties(node)
            splitPane2.revalidate()
            splitPane2.repaint()
        }

        override fun onViewNodeHovered(node: ViewNode) {
            layoutPanel.hoverNode(node)
        }

        override fun onViewNodeNotHovered() {
            layoutPanel.removeNodeHover()
        }

        override fun onNodeHovered(node: ViewNode) {
            treePanel.onNodeHovered(node)
        }

        override fun onNodeSelected(node: ViewNode) {
            treePanel.onNodeSelected(node)
            propertiesPanel.showProperties(node)
            splitPane2.revalidate()
            splitPane2.repaint()
            statusLabel.text = ""
        }

        override fun onMouseExited() {
            treePanel.removeHovered()
        }

        override fun onFound(foundItems: List<ViewNode>) {
            treePanel.highlightFoundItems(foundItems)
        }

        override fun onSelectedFoundItem(node: ViewNode) {
            onNodeSelected(node)
        }

        override fun onFoundDialogClosed() {
            treePanel.removeFoundItemsHighlighting()
        }

        override fun onDistanceCalculated(dimensions: Map<DistanceType, Double>) {
            val sb = StringBuilder(" ")
            var index = 0
            for (dimen in dimensions) {
                when (dimen.key) {
                    DistanceType.LEFT -> sb.append("left = ")
                    DistanceType.RIGHT -> sb.append("right = ")
                    DistanceType.TOP -> sb.append("top = ")
                    DistanceType.BOTTOM -> sb.append("bottom = ")
                }
                if (settingsFacade.shouldShowSizeInDp()) {
                    sb.append("%.2f".format(dimen.value))
                } else {
                    sb.append("${dimen.value.toInt()}")
                }
                if (index < dimensions.size - 1) {
                    sb.append(" ")
                }
                index++
            }
            if (settingsFacade.shouldShowSizeInDp()) {
                sb.append(" (dp)")
            } else {
                sb.append(" (px)")
            }
            statusLabel.text = sb.toString()
        }
    }

    override fun showLoading() {
        loadingDialog.setLocationRelativeTo(this)
        loadingDialog.isVisible = true
    }

    override fun hideLoading() {
        loadingDialog.isVisible = false
        if (settingsFacade.shouldStopAdbAfterJob()) {
            adb.stop()
        }
    }

    internal fun showFindDialog() {
        findDialog.showDialog()
    }

    private fun isPortAlreadyUsed(port: Int): Boolean {
        // Assume no connection is possible.
        try {
            (Socket("localhost", port)).close()
            return true
        } catch (e: SocketException) {
            // Could not connect.
            return false
        }
    }

    private fun initBaseDebugPort() {
        //find at least 10 opened ports
        var openedPortsCount = 0

        for (port in 8600 until 8698) {
            if (isPortAlreadyUsed(port)) {
                openedPortsCount = 0
                logger.d("debug port base $port is busy")
                continue
            }

            if (openedPortsCount >= 10) {
                val firstOpenedPort = port - openedPortsCount
                logger.d("selected debug port base $firstOpenedPort")
                DdmPreferences.setDebugPortBase(firstOpenedPort)
                return
            }
            openedPortsCount++
        }
    }

    override fun showOpenFileDialogAndReturnResult(): File? {
        val fileChooser = JFileChooser(File(settingsFacade.lastLayoutDialogPath))
        fileChooser.preferredSize = Dimension(800, 600)
        fileChooser.addChoosableFileFilter(filter)
        fileChooser.fileFilter = filter

        val returnVal: Int = fileChooser.showOpenDialog(this)

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            val file: File = fileChooser.selectedFile
            settingsFacade.lastLayoutDialogPath = file.parent
            return file
        }
        return null
    }

    fun calculateDistance(selectedValue: ViewNode, targetNode: ViewNode) {
        layoutPanel.calculateDistanceBetweenTwoViewNodes(selectedValue, targetNode)
    }

    companion object {
        // Main Method
        @JvmStatic
        fun main(args: Array<String>) {
            val logger: AppLogger = SimpleConsoleLogger("")
            val settings = JsonSettings(logger)

            // Creating Object of MainWindow class.
            val sl = Main(OpenWindowMode.DEFAULT, settings, logger)
            // Function to set visibilty of JFrame.
            sl.initUi()
        }
    }
}
