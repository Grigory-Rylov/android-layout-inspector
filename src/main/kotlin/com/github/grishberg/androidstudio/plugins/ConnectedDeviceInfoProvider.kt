package com.github.grishberg.androidstudio.plugins

import com.android.ddmlib.IDevice

data class ConnectedDeviceInfo(
    val devices: List<IDevice>
)

interface AdbProvider {
    fun getAdb(): AdbWrapper
}

class ConnectedDeviceInfoProvider(
    private val adbProvider: AdbProvider,
    private val notificationHelper: NotificationHelper
) {
    fun provideDeviceInfo(): ConnectedDeviceInfo? {
        val adb = adbProvider.getAdb()

        if (!adb.isReady()) {
            notificationHelper.error("No platform configured")
            return null
        }

        val devices = adb.connectedDevices()
        if (devices.isEmpty()) {
            notificationHelper.error("No devices found")
        }
        return ConnectedDeviceInfo(devices)
    }
}
