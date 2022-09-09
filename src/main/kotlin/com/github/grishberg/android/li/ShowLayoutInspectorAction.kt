package com.github.grishberg.android.li

import com.android.ddmlib.IDevice
import com.android.layoutinspector.common.AdbFacade
import com.android.layoutinspector.common.PluginLogger
import com.github.grishberg.android.layoutinspector.process.providers.DeviceProvider
import com.github.grishberg.android.layoutinspector.ui.OpenWindowMode
import com.github.grishberg.android.layoutinspector.ui.WindowsManager
import com.github.grishberg.android.layoutinspector.ui.tree.EmptyTreeIcon
import com.github.grishberg.android.li.ui.NotificationHelperImpl
import com.github.grishberg.androidstudio.plugins.AdbProvider
import com.github.grishberg.androidstudio.plugins.AdbWrapper
import com.github.grishberg.androidstudio.plugins.AdbWrapperImpl
import com.github.grishberg.androidstudio.plugins.AsAction
import com.github.grishberg.androidstudio.plugins.ConnectedDeviceInfoProvider
import com.intellij.openapi.actionSystem.AnActionEvent
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
        val provider = ConnectedDeviceInfoProvider(adbProvider, NotificationHelperImpl)

        createUi()

        val main = windowsManager.createWindow(
            OpenWindowMode.DEFAULT,
            settings,
            DeviceProviderImpl(provider),
            AdbFacadeImpl(provider, project.context().adb),
            prepareBaseDir(project)
        )
        main.initUi()
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
        private val provider: ConnectedDeviceInfoProvider
    ) : DeviceProvider {
        override val isReconnectionAllowed: Boolean
            get() = false

        override val deviceChangedActions: MutableSet<DeviceProvider.DeviceChangedAction>
            get() = mutableSetOf()

        override fun reconnect() = Unit

        override suspend fun requestDevices(): List<IDevice> {
            val devicesInfo = provider.provideDeviceInfo() ?: return emptyList()
            return devicesInfo.devices
        }

        override fun stop() = Unit
    }

    private class AdbFacadeImpl(
        private val provider: ConnectedDeviceInfoProvider,
        private val adb: AdbWrapper,
    ) : AdbFacade {
        override fun connect() = Unit

        override fun connect(remoterAddress: String) = Unit

        override fun getDevices(): List<IDevice> {
            val devicesInfo = provider.provideDeviceInfo() ?: return emptyList()
            return devicesInfo.devices
        }

        override fun hasInitialDeviceList(): Boolean = true

        override fun isConnected(): Boolean = adb.isReady()

        override fun stop() = Unit
    }
}
