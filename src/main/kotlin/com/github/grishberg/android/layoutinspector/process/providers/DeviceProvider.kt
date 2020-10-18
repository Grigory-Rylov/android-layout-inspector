package com.github.grishberg.android.layoutinspector.process.providers

import com.android.ddmlib.IDevice

interface DeviceProvider {
    fun stop()
    fun reconnect()
    suspend fun requestDevices(): List<IDevice>
    val deviceChangedActions: MutableSet<DeviceChangedAction>
    val isReconnectionAllowed: Boolean

    interface DeviceChangedAction {
        fun deviceConnected(device: IDevice)
        fun deviceDisconnected(device: IDevice)
    }
}
