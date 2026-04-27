package com.github.grishberg.android.li

import com.android.ddmlib.IDevice
import com.android.layoutinspector.common.AdbFacade
import com.android.layoutinspector.common.AppLogger
import com.android.layoutinspector.common.PluginLogger
import com.github.grishberg.android.layoutinspector.process.providers.DeviceProvider
import com.github.grishberg.android.layoutinspector.settings.SettingsFacade
import com.github.grishberg.android.layoutinspector.ui.OpenWindowMode
import com.github.grishberg.android.layoutinspector.ui.WindowsManager
import com.github.grishberg.android.layoutinspector.ui.tree.EmptyTreeIcon
import com.github.grishberg.android.li.ui.NotificationHelperImpl
import com.github.grishberg.androidstudio.plugins.*
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import java.io.File
import javax.swing.UIManager

private const val PLUGIN_DIR = "captures/YALI"

class ShowLayoutInspectorAction : AsAction() {
    private val windowsManager by lazy { WindowsManager(PluginLogger()) }

    override fun actionPerformed(e: AnActionEvent, project: Project) {
        val settings = StorageService.getInstance().state ?: PluginState()
        val adbProvider = object : AdbProvider {
            override fun getAdb(): AdbWrapper {
                return AdbWrapperImpl(project)
            }
        }
        val notificationHelper = NotificationHelperImpl(project)
        val logger = PluginLogger()
        val provider = ConnectedDeviceInfoProvider(adbProvider, notificationHelper, logger)

        createUi()

        val main = windowsManager.createWindow(
            OpenWindowMode.DEFAULT,
            settings,
            DeviceProviderImpl(provider, logger),
            AdbFacadeImpl(provider, adbProvider.getAdb(), logger),
            prepareBaseDir(project)
        )
        main.initUi()

        showSupportBannerIfNeeded(settings, notificationHelper)
    }

    private fun showSupportBannerIfNeeded(settings: SettingsFacade, notificationHelper: NotificationHelper) {
        val plugin =
            PluginManagerCore.getPlugin(PluginId.getId("com.github.grishberg.android.android-layout-inspector-plugin"))
                ?: return
        if (plugin.version != settings.lastVersion) {
            settings.lastVersion = plugin.version
            notificationHelper.supportInfo(
                "Support me if you like YALI =)",
                "BNB,ETH tokens : 0x25Ca16AD3c4e9BD1e6e5FDD77eDB019386B68591\n\n" +
                        "USDT TRC20 : TSo3X6K54nYq3S64wML4M4xFgTNiENkHwC\n\n" +
                        "BTC : bc1qmm5lp389scuk2hghgyzdztddwgjnxqa2awrrue\n\n" +
                        "https://www.tinkoff.ru/cf/4KNjR2SMOAj"
            )
        }
    }

    private fun prepareBaseDir(project: Project): File {
        val baseDir = if (project.basePath != null)
            File(project.basePath, PLUGIN_DIR)
        else
            File(PLUGIN_DIR)

        if (!baseDir.exists()) {
            baseDir.mkdirs()
        }

        return baseDir
    }

    private fun createUi() {
        val emptyIcon = EmptyTreeIcon()
        UIManager.put("Tree.leafIcon", emptyIcon)
    }

    private class DeviceProviderImpl(
        private val provider: ConnectedDeviceInfoProvider,
        private val logger: AppLogger
    ) : DeviceProvider {
        override val isReconnectionAllowed: Boolean
            get() = false

        override val deviceChangedActions: MutableSet<DeviceProvider.DeviceChangedAction>
            get() = mutableSetOf()

        override fun attachLogger(newLogger: AppLogger) {
            provider.attachLogger(newLogger)
        }
        
        override fun reconnect() = Unit

        override suspend fun requestDevices(): List<IDevice> {
            logger.d("=== DeviceProviderImpl.requestDevices() called ===")
            val devicesInfo = provider.provideDeviceInfo()
            logger.d("DeviceProviderImpl.requestDevices() returned ${devicesInfo?.devices?.size ?: 0} devices")
            return devicesInfo?.devices ?: emptyList()
        }

        override fun stop() = Unit
    }

    private class AdbFacadeImpl(
        private val provider: ConnectedDeviceInfoProvider,
        private val adb: AdbWrapper,
        private val logger: AppLogger
    ) : AdbFacade {
        override fun connect() = Unit

        override fun connect(remoterAddress: String) = Unit

        override fun getDevices(): List<IDevice> {
            logger.d("=== AdbFacadeImpl.getDevices() called ===")
            val devicesInfo = provider.provideDeviceInfo()
            logger.d("AdbFacadeImpl.getDevices() returning ${devicesInfo?.devices?.size ?: 0} devices")
            return devicesInfo?.devices ?: emptyList()
        }

        override fun hasInitialDeviceList(): Boolean = true

        override fun isConnected(): Boolean = adb.isReady()

        override fun stop() = Unit
    }
}
