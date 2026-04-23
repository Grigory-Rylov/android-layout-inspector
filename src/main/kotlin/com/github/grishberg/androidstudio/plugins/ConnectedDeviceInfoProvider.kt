package com.github.grishberg.androidstudio.plugins

import com.android.ddmlib.IDevice
import com.android.layoutinspector.common.AppLogger

data class ConnectedDeviceInfo(
    val devices: List<IDevice>
)

interface AdbProvider {
    fun getAdb(): AdbWrapper
}

class ConnectedDeviceInfoProvider(
    private val adbProvider: AdbProvider,
    private val notificationHelper: NotificationHelper,
    private val logger: AppLogger
) {
    companion object {
        private const val TAG = "ConnectedDeviceInfoProvider"
    }

    fun provideDeviceInfo(): ConnectedDeviceInfo? {
        logger.d("$TAG: provideDeviceInfo() - started")
        val adb = adbProvider.getAdb()

        if (!adb.isReady()) {
            logger.w("$TAG: provideDeviceInfo() - ADB is not ready")
            notificationHelper.error("No platform configured")
            return null
        }

        logger.d("$TAG: provideDeviceInfo() - ADB is ready, requesting devices")
        val devices = adb.connectedDevices()
        logger.d("$TAG: provideDeviceInfo() - received ${devices.size} devices")
        for (device in devices) {
            logger.d("$TAG: provideDeviceInfo() - device: ${device.serialNumber} (${device.name})")
        }
        if (devices.isEmpty()) {
            logger.w("$TAG: provideDeviceInfo() - no devices found")
            notificationHelper.error("No devices found")
        }
        return ConnectedDeviceInfo(devices)
    }
}
