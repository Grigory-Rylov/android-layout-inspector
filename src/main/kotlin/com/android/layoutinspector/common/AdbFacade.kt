package com.android.layoutinspector.common

import com.android.ddmlib.IDevice

interface AdbFacade {
    fun isConnected(): Boolean
    fun getDevices(): List<IDevice>
    fun hasInitialDeviceList(): Boolean
    fun connect()
    fun connect(remoterAddress: String)
    fun stop()
}
