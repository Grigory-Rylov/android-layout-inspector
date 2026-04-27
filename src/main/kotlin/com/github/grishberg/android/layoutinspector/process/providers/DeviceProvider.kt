package com.github.grishberg.android.layoutinspector.process.providers

import com.android.ddmlib.IDevice
import com.android.layoutinspector.common.AppLogger


interface DeviceProvider {
    val deviceChangedActions: MutableSet<DeviceChangedAction>
    val isReconnectionAllowed: Boolean

    fun stop()
    fun reconnect()
    fun attachLogger(newLogger: AppLogger) {}
    suspend fun requestDevices(): List<IDevice>

    interface DeviceChangedAction {
        fun deviceConnected(device: IDevice)
        fun deviceDisconnected(device: IDevice)
    }
}
