package com.github.grishberg.android.layoutinspector.process

import com.android.ddmlib.IDevice
import com.android.layoutinspector.common.AdbFacade
import com.github.grishberg.android.layoutinspector.settings.SETTINGS_ANDROID_HOME
import com.github.grishberg.android.layoutinspector.settings.Settings
import com.github.grishberg.android.layoutinspector.ui.InspectorLogger
import com.github.grishberg.tracerecorder.adb.AdbWrapperImpl

class AdbFacadeImpl(settings: Settings) : AdbFacade {
    private val adb by lazy {
        AdbWrapperImpl(
            clientSupport = true,
            logger = InspectorLogger(),
            androidHome = settings.getStringValue(SETTINGS_ANDROID_HOME)
        )
    }

    override fun isConnected(): Boolean = adb.isConnected()

    override fun getDevices(): List<IDevice> = adb.getDevices()

    override fun connect() = adb.connect()

    override fun connect(remoterAddress: String) = adb.connect(remoterAddress)

    override fun stop() = adb.stop()

    override fun hasInitialDeviceList(): Boolean = adb.hasInitialDeviceList()
}
