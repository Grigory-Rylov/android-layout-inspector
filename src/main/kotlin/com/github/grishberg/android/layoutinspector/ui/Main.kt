package com.github.grishberg.android.layoutinspector.ui

import com.android.layoutinspector.common.AdbFacade
import com.android.layoutinspector.common.AppLogger
import com.android.layoutinspector.model.LayoutFileData
import com.android.layoutinspector.model.ViewNode
import com.github.grishberg.android.layoutinspector.common.CoroutinesDispatchersImpl
import com.github.grishberg.android.layoutinspector.common.MainScope
import com.github.grishberg.android.layoutinspector.domain.DialogsInput
import com.github.grishberg.android.layoutinspector.domain.LayoutResultOutput
import com.github.grishberg.android.layoutinspector.domain.Logic
import com.github.grishberg.android.layoutinspector.domain.MetaRepository
import com.github.grishberg.android.layoutinspector.process.LayoutFileSystem
import com.github.grishberg.android.layoutinspector.process.LayoutParserImpl
import com.github.grishberg.android.layoutinspector.process.providers.ClientWindowsProvider
import com.github.grishberg.android.layoutinspector.process.providers.DeviceProvider
import com.github.grishberg.android.layoutinspector.settings.SettingsFacade
import com.github.grishberg.android.layoutinspector.ui.common.createAccelerator
import com.github.grishberg.android.layoutinspector.ui.common.createControlAccelerator
import com.github.grishberg.android.layoutinspector.ui.common.createControlShiftAccelerator
import com.github.grishberg.android.layoutinspector.ui.dialogs.FindDialog
import com.github.grishberg.android.layoutinspector.ui.dialogs.LoadingDialog
import com.github.grishberg.android.layoutinspector.ui.dialogs.LoadingDialogClosedEventListener
import com.github.grishberg.android.layoutinspector.ui.dialogs.NewLayoutDialog
import com.github.grishberg.android.layoutinspector.ui.dialogs.WindowsDialog
import com.github.grishberg.android.layoutinspector.ui.dialogs.bookmarks.Bookmarks
import com.github.grishberg.android.layoutinspector.ui.info.PropertiesPanel
import com.github.grishberg.android.layoutinspector.ui.info.flat.FlatPropertiesWithFilterPanel
import com.github.grishberg.android.layoutinspector.ui.info.flat.filter.SimpleFilterTextView
import com.github.grishberg.android.layoutinspector.ui.layout.DistanceType
import com.github.grishberg.android.layoutinspector.ui.layout.ImageTransferable
import com.github.grishberg.android.layoutinspector.ui.layout.LayoutLogic
import com.github.grishberg.android.layoutinspector.ui.layout.LayoutPanel
import com.github.grishberg.android.layoutinspector.ui.layout.LayoutsEnabledState
import com.github.grishberg.android.layoutinspector.ui.screenshottest.ScreenshotTestDialog
import com.github.grishberg.android.layoutinspector.ui.theme.ThemeProxy
import com.github.grishberg.android.layoutinspector.ui.theme.Themes
import com.github.grishberg.android.layoutinspector.ui.tree.TreePanel
import com.intellij.ui.components.JBScrollPane
import java.awt.AWTException
import java.awt.BorderLayout
import java.awt.Desktop
import java.awt.Dimension
import java.awt.Toolkit
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.image.BufferedImage
import java.io.File
import java.net.URI
import javax.swing.AbstractAction
import javax.swing.AbstractButton
import javax.swing.BoxLayout
import javax.swing.ButtonGroup
import javax.swing.JCheckBoxMenuItem
import javax.swing.JComponent
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JMenuItem
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JRadioButtonMenuItem
import javax.swing.JScrollPane
import javax.swing.JSplitPane
import javax.swing.JToolBar
import javax.swing.KeyStroke
import javax.swing.SwingConstants
import javax.swing.border.BevelBorder
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.math.roundToInt


private const val INITIAL_SCREEN_WIDTH = 1024
private const val INITIAL_SCREEN_HEIGHT = 800
private const val INITIAL_LAYOUTS_WINDOW_WIDTH = 300
private const val INITIAL_PROPERTIES_WINDOW_WIDTH = 400

enum class OpenWindowMode {
    DEFAULT,
    OPEN_FILE,
}

enum class Actions {
    RESET_ZOOM, FIT_TO_SCREEN, HELP, REFRESH
}

// create a class MainWindow extending JFrame
class Main(
    private val windowsManager: WindowsManager,
    private val mode: OpenWindowMode,
    private val settingsFacade: SettingsFacade,
    private val logger: AppLogger,
    private val deviceProvider: DeviceProvider,
    private val adb: AdbFacade,
    private val baseDir: File
) : JFrame("Yet Another Android Layout Inspector."),
    LayoutResultOutput, DialogsInput {

    // Declaration of objects of the
    // JScrollPane class.
    private val layoutPanel: LayoutPanel
    private val treePanel: TreePanel
    private val propertiesPanel: PropertiesPanel
    private val logic: Logic
    private val statusLabel: JLabel

    private val splitPane1: JSplitPane
    private val splitPane2: JSplitPane
    private val fileMenu: JMenu
    private val loadingDialog: LoadingDialog
    private val windowsDialog: WindowsDialog
    private val clientWindowsProvider: ClientWindowsProvider
    private val findDialog: FindDialog

    private val mainPanel: JPanel
    private val statusDistanceLabel: JLabel

    private val themeProxy = ThemeProxy()
    private val themes: Themes
    private val filter = FileNameExtensionFilter("Layout inspector files", "li")
    private val bookmarks = Bookmarks()
    private val metaRepository = MetaRepository(logger, bookmarks, baseDir)
    private val layoutsState = LayoutsEnabledState()
    private val toggleShowingLayouts = JCheckBoxMenuItem("Toggle showing layouts")

    // Constructor of MainWindow class
    init {
        themes = Themes(
            this,
            themeProxy,
            logger
        )

        // on close window the close method is called
        defaultCloseOperation = DO_NOTHING_ON_CLOSE
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(evt: WindowEvent) {
                close()
            }
        })

        layoutPanel = LayoutPanel(metaRepository, settingsFacade, layoutsState, logger)
        treePanel = TreePanel(this, themeProxy, metaRepository, bookmarks, main = this)
        propertiesPanel = FlatPropertiesWithFilterPanel(
            metaRepository,
            settingsFacade,
            themeProxy,
            SimpleFilterTextView(settingsFacade),
            logger
        )
        propertiesPanel.setSizeDpMode(settingsFacade.shouldShowSizeInDp())
        layoutPanel.setSizeDpMode(settingsFacade.shouldShowSizeInDp())

        val selectionAction = TreeNodeSelectedAction()
        treePanel.nodeSelectedAction = selectionAction
        layoutPanel.setOnLayoutSelectedAction(selectionAction)
        findDialog = FindDialog(this)
        findDialog.foundAction = selectionAction

        val treeScrollPane = JBScrollPane(
            treePanel,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS
        )

        val toolbar = JToolBar()
        toolbar.isFloatable = false
        ButtonsBuilder(layoutPanel, this, themes).addToolbarButtons(toolbar)

        mainPanel = JPanel(BorderLayout())
        mainPanel.add(toolbar, BorderLayout.NORTH)
        splitPane1 = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, layoutPanel, treeScrollPane)
        splitPane2 = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, splitPane1, propertiesPanel.getComponent())
        splitPane2.resizeWeight = 1.0
        splitPane1.dividerLocation = INITIAL_LAYOUTS_WINDOW_WIDTH
        splitPane2.dividerLocation = INITIAL_SCREEN_WIDTH - (INITIAL_PROPERTIES_WINDOW_WIDTH)
        mainPanel.add(splitPane2, BorderLayout.CENTER)


        statusLabel = JLabel()
        mainPanel.add(statusLabel, BorderLayout.SOUTH)

        statusDistanceLabel = JLabel()
        createStatusBar(statusLabel)

        contentPane = mainPanel
        pack()

        windowsDialog = WindowsDialog(this, settingsFacade, logger)
        clientWindowsProvider = ClientWindowsProvider(logger)

        val devicesInputDialog = NewLayoutDialog(this, deviceProvider, logger, settingsFacade)

        val coroutineScope = MainScope()
        val coroutinesDispatchers = CoroutinesDispatchersImpl()

        val fileSystem = LayoutFileSystem(logger, baseDir)
        logic = Logic(
            devicesInputDialog,
            windowsDialog,
            clientWindowsProvider,
            LayoutParserImpl(),
            this,
            logger,
            fileSystem,
            this,
            metaRepository,
            coroutineScope,
            coroutinesDispatchers
        )

        loadingDialog = LoadingDialog(this, object : LoadingDialogClosedEventListener {
            override fun onLoadingDialogClosed() {
                logic.onLoadingDialogClosed()
            }
        })

        fileMenu = createFileMenu(fileSystem)
        createMenu(fileMenu)


        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "Cancel"
        )

        getRootPane().actionMap.put("Cancel", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                if (!logic.hasOpenedLayouts()) {
                    close()
                    return
                }

                if (!layoutPanel.hasSelection()) {
                    val ObjButtons = arrayOf("Yes", "No")
                    val PromptResult = JOptionPane.showOptionDialog(
                        null,
                        "Are you sure you want to exit?",
                        "Closing YALI",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.WARNING_MESSAGE,
                        null,
                        ObjButtons,
                        ObjButtons[1]
                    )
                    if (PromptResult == JOptionPane.YES_OPTION) {
                        close()
                    }

                    return
                }

                layoutPanel.removeSelection()
                treePanel.clearSelection()
            }
        })

        setSize(INITIAL_SCREEN_WIDTH, INITIAL_SCREEN_HEIGHT)
        defaultCloseOperation = DISPOSE_ON_CLOSE
        setLocationRelativeTo(null)
        isVisible = true
    }

    private fun close() {
        windowsManager.onDestroyed(this)
        isVisible = false
        dispose()
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

    private fun createMenu(fileMenu: JMenu) {
        val menuBar = JMenuBar()
        menuBar.add(fileMenu)
        menuBar.add(createViewMenu())
        menuBar.add(createToolsMenu())
        menuBar.add(createSettingsMenu())
        jMenuBar = menuBar
    }

    private fun createFileMenu(fileSystem: LayoutFileSystem): JMenu {
        val file = JMenu("File")

        val openFile = JMenuItem("Open")
        file.add(openFile)
        openFile.addActionListener { arg0: ActionEvent? -> openExistingFile() }
        openFile.accelerator = createControlAccelerator('O')

        val openFileInNewWindow = JMenuItem("Open in new window")
        file.add(openFileInNewWindow)
        openFileInNewWindow.addActionListener { arg0: ActionEvent? -> openExistingFile(newWindow = true) }

        val newFile = JMenuItem("Record new Layout info")
        file.add(newFile)
        newFile.addActionListener { arg0: ActionEvent? -> startRecording() }
        newFile.accelerator = createControlAccelerator('N')

        val recordInNewWindow = JMenuItem("Record new Layout in new window")
        file.add(recordInNewWindow)
        recordInNewWindow.addActionListener { arg0: ActionEvent? -> startRecording(newWindow = true) }
        recordInNewWindow.accelerator = createControlShiftAccelerator('N')

        val openLayoutsFolder = JMenuItem("Open layouts folder")
        file.add(openLayoutsFolder)
        openLayoutsFolder.addActionListener { arg: ActionEvent? ->
            openLayoutsDirInExternalFileManager(fileSystem)
        }

        file.addSeparator()
        return file
    }

    private fun openLayoutsDirInExternalFileManager(fileSystem: LayoutFileSystem) {
        val desktop = Desktop.getDesktop()
        try {
            desktop.open(fileSystem.layoutDir)
        } catch (e: Exception) {
            logger.e("File Not Found", e)
        }
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

        val resetZoom = JMenuItem("Reset zoom")
        viewMenu.add(resetZoom)
        resetZoom.addActionListener { layoutPanel.resetZoom() }
        resetZoom.accelerator = createAccelerator('Z')

        val fitScreen = JMenuItem("Fit to screen")
        viewMenu.add(fitScreen)
        fitScreen.addActionListener { layoutPanel.fitZoom() }
        fitScreen.accelerator = createAccelerator('F')

        viewMenu.addSeparator()

        val openFind = JMenuItem("Find")
        viewMenu.add(openFind)
        openFind.addActionListener { arg0: ActionEvent? -> showFindDialog() }

        val showSelectedSerifs = JCheckBoxMenuItem("Show selected element serifs")
        showSelectedSerifs.isSelected = settingsFacade.showSerifsInTheMiddleOfSelected
        showSelectedSerifs.addActionListener { e ->
            val aButton = e.source as AbstractButton
            settingsFacade.showSerifsInTheMiddleOfSelected = aButton.model.isSelected
            layoutPanel.repaint()
        }
        viewMenu.add(showSelectedSerifs)

        val showAllSerifs = JCheckBoxMenuItem("Show all element serifs")
        showAllSerifs.isSelected = settingsFacade.showSerifsInTheMiddleAll
        showAllSerifs.addActionListener { e ->
            val aButton = e.source as AbstractButton
            settingsFacade.showSerifsInTheMiddleAll = aButton.model.isSelected
            layoutPanel.repaint()
        }
        viewMenu.add(showAllSerifs)


        toggleShowingLayouts.isSelected = true
        toggleShowingLayouts.accelerator = createAccelerator('L')
        toggleShowingLayouts.addActionListener { e ->
            val aButton = e.source as AbstractButton
            layoutsState.isEnabled = aButton.model.isSelected
            layoutPanel.repaint()
        }
        viewMenu.add(toggleShowingLayouts)

        return viewMenu
    }

    private fun createToolsMenu(): JMenu {
        val toolsMenu = JMenu("Tools")
        val compareScreenShot = JMenuItem("Screenshot test")
        compareScreenShot.addActionListener { tryToStartScreenshotTest() }
        compareScreenShot.accelerator = createAccelerator('S')
        toolsMenu.add(compareScreenShot)

        val copyScreenShot = JMenuItem("Copy screenshot to clipboard")
        copyScreenShot.addActionListener { copyScreenshotToClipboard() }
        toolsMenu.add(copyScreenShot)

        return toolsMenu
    }

    private fun createSettingsMenu(): JMenu {
        val settingsMenu = JMenu("Settings")

        val allowSelectNotDrawnView = JCheckBoxMenuItem("Allow select hidden view")
        allowSelectNotDrawnView.isSelected = settingsFacade.allowedSelectHiddenView
        allowSelectNotDrawnView.addActionListener { e ->
            val aButton = e.source as AbstractButton
            settingsFacade.allowedSelectHiddenView = aButton.model.isSelected
        }
        settingsMenu.add(allowSelectNotDrawnView)

        val ignoreLastClickedView = JCheckBoxMenuItem("Select parent view on next click")
        ignoreLastClickedView.isSelected = settingsFacade.ignoreLastClickedView
        ignoreLastClickedView.addActionListener { e ->
            val aButton = e.source as AbstractButton
            settingsFacade.ignoreLastClickedView = aButton.model.isSelected
        }
        settingsMenu.add(ignoreLastClickedView)


        val roundDimensions = JCheckBoxMenuItem("Round dimensions")
        roundDimensions.isSelected = settingsFacade.roundDimensions
        roundDimensions.addActionListener { e ->
            val aButton = e.source as AbstractButton
            settingsFacade.roundDimensions = aButton.model.isSelected
            invalidateDimensions()
        }
        settingsMenu.add(roundDimensions)
        return settingsMenu
    }

    private fun invalidateDimensions() {
        propertiesPanel.roundDp(settingsFacade.roundDimensions)
        layoutPanel.roundDp()
    }

    private fun setSizeDpMode(enabled: Boolean) {
        statusLabel.text = ""
        settingsFacade.showSizeInDp(enabled)
        propertiesPanel.setSizeDpMode(enabled)
        layoutPanel.setSizeDpMode(enabled)
    }

    private fun startRecording(newWindow: Boolean = false) {
        if (newWindow) {
            val main = windowsManager.createWindow(
                OpenWindowMode.DEFAULT,
                settingsFacade,
                deviceProvider,
                adb,
                baseDir
            )
            main.initUi()
            return
        }
        logic.startRecording()
    }

    private fun tryToStartScreenshotTest() {
        val result = windowsManager.startScreenshotTest(this)
        if (!result) {
            JOptionPane.showMessageDialog(
                this,
                "Can't start screenshot test. Do you have two windows with layouts from same device?",
            )
        }
    }

    fun openExistingFile(newWindow: Boolean = false) {
        if (newWindow) {
            val main = windowsManager.createWindow(
                OpenWindowMode.OPEN_FILE,
                settingsFacade,
                deviceProvider,
                adb,
                baseDir
            )
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
                    if (settingsFacade.roundDimensions) {
                        sb.append("${dimen.value.roundToInt()}")
                    } else {
                        sb.append("%.2f".format(dimen.value))
                    }
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

    fun goToHelp() {
        openWebpage("https://github.com/Grigory-Rylov/android-layout-inspector")
    }

    private fun openWebpage(uri: String): Boolean {
        val desktop = if (Desktop.isDesktopSupported()) Desktop.getDesktop() else null
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                desktop.browse(URI.create(uri))
                return true
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
        return false
    }

    fun screenshot(): BufferedImage? {
        return layoutPanel.screenshot
    }

    fun screenshotTest(
        referenceScreenshot: BufferedImage,
        otherBufferedImage: BufferedImage,
    ) {
        val screenshotTestDialog = ScreenshotTestDialog(
            this, layoutPanel.screenshotPainter
        )
        screenshotTestDialog.showDialog(
            referenceScreenshot,
            otherBufferedImage,
        )
    }

    fun copyScreenshotToClipboard() {
        layoutPanel.copyScreenshotToClipboard()
    }

    fun toggleShowingLayouts() {
        toggleShowingLayouts.model.isSelected = !toggleShowingLayouts.model.isSelected
        layoutsState.isEnabled = toggleShowingLayouts.model.isSelected
        layoutPanel.repaint()
    }
}
