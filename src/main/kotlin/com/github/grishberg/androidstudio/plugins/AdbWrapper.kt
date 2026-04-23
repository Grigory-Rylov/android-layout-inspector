package com.github.grishberg.androidstudio.plugins

import com.android.ddmlib.IDevice
import com.android.layoutinspector.common.AppLogger
import com.android.layoutinspector.common.PluginLogger
import com.intellij.openapi.project.Project
import org.jetbrains.android.sdk.AndroidSdkUtils

interface AdbWrapper {
    fun isReady(): Boolean
    fun connectedDevices(): List<IDevice>
}

class AdbWrapperImpl(project: Project) : AdbWrapper {
    companion object {
        private const val TAG = "AdbWrapper"
    }
    private val logger: AppLogger = PluginLogger()
    val androidBridge = AndroidSdkUtils.getDebugBridge(project)

    override fun isReady(): Boolean {
        logger.d("$TAG: isReady() - androidBridge = ${androidBridge != null}")
        if (androidBridge == null) {
            return false
        }

        val isConnected = androidBridge.isConnected
        val hasInitialDeviceList = androidBridge.hasInitialDeviceList()
        logger.d("$TAG: isReady() - isConnected = $isConnected, hasInitialDeviceList = $hasInitialDeviceList")
        return isConnected && hasInitialDeviceList
    }

    override fun connectedDevices(): List<IDevice> {
        val devices = androidBridge?.devices?.asList() ?: emptyList()
        logger.d("$TAG: connectedDevices() - returning ${devices.size} devices")
        for (device in devices) {
            logger.d("$TAG: connectedDevices() - device: ${device.serialNumber} (${device.name})")
        }
        return devices
    }
}
