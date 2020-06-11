package com.github.grishberg.android.layoutinspector.settings

import com.github.grishberg.android.layoutinspector.ui.Theme

private const val SETTINGS_SHOULD_STOP_ADB = "shouldStopAdbAfterJob"
private const val SETTINGS_SIZE_IN_DP = "sizeInDp"
const val SETTINGS_ANDROID_HOME = "androidHome"
private const val SETTINGS_ALLOW_SELECT_HIDDEN_VIEW = "allowSelectHiddenView"

private const val SETTINGS_TIMEOUT = "timeoutInSeconds"
private const val SETTINGS_ADB_INITIAL_REMOTE_ADDRESS = "remoteDeviceAddress"
private const val SETTINGS_WAIT_FOR_CLIENT_WINDOWS_TIMEOUT = "clientWindowsTimeout"

private const val SETTINGS_THEME = "theme"

class SettingsFacade(
    private val settings: Settings
) {
    var androidHome: String
        get() = settings.getStringValueOrDefault(SETTINGS_ANDROID_HOME, "")
        set(value) = settings.setStringValue(SETTINGS_ANDROID_HOME, value)

    var captureLayoutTimeout: Long
        set(value) = settings.setIntValue(SETTINGS_TIMEOUT, value.toInt())
        get() = settings.getIntValueOrDefault(SETTINGS_TIMEOUT, 60).toLong()

    var clientWindowsTimeout: Long
        set(value) = settings.setIntValue(SETTINGS_WAIT_FOR_CLIENT_WINDOWS_TIMEOUT, value.toInt())
        get() = settings.getIntValueOrDefault(SETTINGS_WAIT_FOR_CLIENT_WINDOWS_TIMEOUT, 10).toLong()

    var remoteDeviceAddress: String
        set(value) = settings.setStringValue(SETTINGS_ADB_INITIAL_REMOTE_ADDRESS, value)
        get() = settings.getStringValueOrDefault(SETTINGS_ADB_INITIAL_REMOTE_ADDRESS, "")

    var allowedSelectHiddenView: Boolean
        get() = settings.getBoolValueOrDefault(SETTINGS_ALLOW_SELECT_HIDDEN_VIEW)
        set(value) = settings.setBoolValue(SETTINGS_ALLOW_SELECT_HIDDEN_VIEW, value)

    var theme: Theme
        get() {
            val storedThemeName = settings.getStringValueOrDefault(SETTINGS_THEME, Theme.LITE.name)
            return Theme.valueOf(storedThemeName)
        }
        set(value) {
            settings.setStringValue(SETTINGS_THEME, value.name)
        }

    init {
        // create default values
        setStopAdbAfterJob(shouldStopAdbAfterJob())
        showSizeInDp(shouldShowSizeInDp())
        allowedSelectHiddenView = settings.getBoolValueOrDefault(SETTINGS_ALLOW_SELECT_HIDDEN_VIEW)
        remoteDeviceAddress = settings.getStringValueOrDefault(SETTINGS_ADB_INITIAL_REMOTE_ADDRESS, "")
    }

    fun shouldStopAdbAfterJob(): Boolean = settings.getBoolValueOrDefault(SETTINGS_SHOULD_STOP_ADB)

    fun setStopAdbAfterJob(state: Boolean) {
        settings.setBoolValue(SETTINGS_SHOULD_STOP_ADB, state)
    }

    fun shouldShowSizeInDp(): Boolean = settings.getBoolValueOrDefault(SETTINGS_SIZE_IN_DP)

    fun showSizeInDp(state: Boolean) {
        settings.setBoolValue(SETTINGS_SIZE_IN_DP, state)
    }
}
