package com.github.grishberg.android.layoutinspector.process.providers

import com.android.ddmlib.IDevice
import com.github.grishberg.tracerecorder.adb.AdbWrapper
import com.github.grishberg.tracerecorder.exceptions.DeviceTimeoutException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

class DeviceProvider(
    private val adb: AdbWrapper
) {
    private val timeout = 30

    suspend fun requestDevices(): List<IDevice> {
        val result = GlobalScope.async(Dispatchers.IO) {
            if (!adb.isConnected()) {
                adb.connect()
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
            } catch (ignored: InterruptedException) {
            }
            if (count > timeout * 10) {
                adb.stop()
                throw DeviceTimeoutException()
            }
        }
    }

}
