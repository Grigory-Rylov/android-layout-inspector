package com.github.grishberg.android.li

import com.android.ddmlib.IDevice
import com.android.layoutinspector.common.AdbFacade
import com.android.layoutinspector.common.SimpleConsoleLogger
import com.github.grishberg.android.layoutinspector.process.providers.DeviceProvider
import com.github.grishberg.android.layoutinspector.settings.JsonSettings
import com.github.grishberg.android.layoutinspector.ui.Main
import com.github.grishberg.android.layoutinspector.ui.OpenWindowMode
import com.github.grishberg.android.layoutinspector.ui.tree.EmptyTreeIcon
import com.github.grishberg.android.li.ui.NotificationHelperImpl
import com.github.grishberg.androidstudio.plugins.AdbWrapper
import com.github.grishberg.androidstudio.plugins.AsAction
import com.github.grishberg.androidstudio.plugins.ConnectedDeviceInfo
import com.github.grishberg.androidstudio.plugins.ConnectedDeviceInfoProvider
import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import javax.swing.UIManager

class ShowLayoutInspectorAction : AsAction() {
    override fun actionPerformed(e: AnActionEvent, project: Project) {
        val provider = ConnectedDeviceInfoProvider(project, project.context().adb, NotificationHelperImpl)
        val deviceInfo = provider.provideDeviceInfo() ?: return

        val log = SimpleConsoleLogger("")
        val settings = JsonSettings(log)

        createUi()
        val main = Main(
            OpenWindowMode.DEFAULT,
            settings,
            log,
            DeviceProviderImpl(deviceInfo),
            AdbFacadeImpl(deviceInfo, project.context().adb)
        )
        main.initUi()

        val noti = NotificationGroup(
            "myplugin",
            NotificationDisplayType.BALLOON, true
        )
        noti.createNotification(
            "My Title",
            "My Message",
            NotificationType.INFORMATION,
            null
        ).notify(e.project)
    }

    private fun createUi() {
        val emptyIcon = EmptyTreeIcon()
        UIManager.put("Tree.leafIcon", emptyIcon)
    }

    private class DeviceProviderImpl(
        private val deviceInfo: ConnectedDeviceInfo
    ) : DeviceProvider {
        override val isReconnectionAllowed: Boolean
            get() = false

        override val deviceChangedActions: MutableSet<DeviceProvider.DeviceChangedAction>
            get() = mutableSetOf()

        override fun reconnect() = Unit

        override suspend fun requestDevices(): List<IDevice> = deviceInfo.devices

        override fun stop() = Unit
    }

    private class AdbFacadeImpl(
        private val deviceInfo: ConnectedDeviceInfo,
        private val adb: AdbWrapper,
    ) : AdbFacade {
        override fun connect() = Unit

        override fun connect(remoterAddress: String) = Unit

        override fun getDevices(): List<IDevice> = deviceInfo.devices

        override fun hasInitialDeviceList(): Boolean = true

        override fun isConnected(): Boolean = adb.isReady()

        override fun stop() = Unit
    }
}
