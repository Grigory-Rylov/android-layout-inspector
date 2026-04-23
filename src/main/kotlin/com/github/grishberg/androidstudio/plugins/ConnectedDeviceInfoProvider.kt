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
    fun provideDeviceInfo(): ConnectedDeviceInfo? {
        logger.d("=== provideDeviceInfo() called ===")
        val adb = adbProvider.getAdb()
        logger.d("AdbWrapper obtained: ${adb.hashCode()}")

        logger.d("Checking adb.isReady()...")
        if (!adb.isReady()) {
            logger.d("adb.isReady() returned false")
            notificationHelper.error("No platform configured")
            return null
        }
        logger.d("adb.isReady() returned true")

        logger.d("Calling adb.connectedDevices()...")
        val devices = adb.connectedDevices()
        logger.d("adb.connectedDevices() returned ${devices.size} devices")
        
        if (devices.isEmpty()) {
            logger.d("Device list is empty - will show error notification")
            notificationHelper.error("No devices found")
        } else {
            logger.d("Devices found:")
            devices.forEach { device ->
                logger.d("  - Device: ${device.name}, serial: ${device.serialNumber}, state: ${device.state}")
            }
        }
        logger.d("=== provideDeviceInfo() returning ${devices.size} devices ===")
        return ConnectedDeviceInfo(devices)
    }
}
