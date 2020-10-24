package com.github.grishberg.androidstudio.plugins

import com.android.ddmlib.IDevice

data class ConnectedDeviceInfo(
    val devices: List<IDevice>
)

class ConnectedDeviceInfoProvider(
    private val adb: AdbWrapper,
    private val notificationHelper: NotificationHelper
) {
    fun provideDeviceInfo(): ConnectedDeviceInfo? {
        if (!adb.isReady()) {
            notificationHelper.error("No platform configured")
            return null
        }

        val devices = adb.connectedDevices()
        return if (devices.isNotEmpty()) {
            ConnectedDeviceInfo(devices)
        } else {
            null
        }
    }
}
