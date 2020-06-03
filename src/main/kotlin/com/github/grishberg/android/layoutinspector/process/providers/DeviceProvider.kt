package com.github.grishberg.android.layoutinspector.process.providers

import com.android.ddmlib.AndroidDebugBridge
import com.android.ddmlib.AndroidDebugBridge.IDeviceChangeListener
import com.android.ddmlib.IDevice
import com.android.layoutinspector.common.AppLogger
import com.android.layoutinspector.settings.Settings
import com.github.grishberg.tracerecorder.adb.AdbWrapper
import com.github.grishberg.tracerecorder.exceptions.DeviceTimeoutException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

private const val TAG = "DeviceProvider"
private const val SETTINGS_ADB_INITIAL_DEVICE_ADDRESS = "adbConnectToAddress"

class DeviceProvider(
    private val logger: AppLogger,
    private val adb: AdbWrapper,
    private val settings: Settings
) {
    private val timeout = 30
    val deviceChangedActions = mutableListOf<DeviceChangedAction>()

    init {
        AndroidDebugBridge.addDeviceChangeListener(object : IDeviceChangeListener {
            // this gets invoked on another thread, but you probably shouldn't count on it
            override fun deviceConnected(device: IDevice) {
                logger.d("$TAG: connected $device")
                deviceChangedActions.forEach {
                    it.deviceConnected(device)
                }
            }

            override fun deviceDisconnected(device: IDevice) {
                deviceChangedActions.forEach {
                    logger.d("$TAG: connected $device")
                    it.deviceDisconnected(device)
                }
            }

            override fun deviceChanged(device: IDevice, changeMask: Int) {
                logger.d("$TAG: deviceChanged $device")
            }
        })
    }

    suspend fun requestDevices(): List<IDevice> {
        val result = GlobalScope.async(Dispatchers.IO) {
            if (!adb.isConnected()) {

                val initialConnectionDeviceAddress = settings.getStringValue(SETTINGS_ADB_INITIAL_DEVICE_ADDRESS)
                if (initialConnectionDeviceAddress != null && initialConnectionDeviceAddress.isNotEmpty()) {
                    adb.connect(initialConnectionDeviceAddress)
                } else {
                    adb.connect()
                }
                waitForDevices(adb)
            }
            return@async adb.getDevices()
        }

        return result.await()
    }

    private fun waitForDevices(adb: AdbWrapper) {
        var count = 0
        while (!adb.hasInitialDeviceList()) {
            try {
                Thread.sleep(100)
                count++
            } catch (ignored: Throwable) {
            }
            if (count > timeout * 10) {
                adb.stop()
                throw DeviceTimeoutException()
            }
        }
    }

    interface DeviceChangedAction {
        fun deviceConnected(device: IDevice)
        fun deviceDisconnected(device: IDevice)
    }
}
