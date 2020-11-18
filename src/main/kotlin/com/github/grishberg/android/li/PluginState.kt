package com.github.grishberg.android.li

import com.github.grishberg.android.layoutinspector.settings.SettingsFacade

class PluginState : SettingsFacade {
    var shouldShowSizeInDp = false

    override var captureLayoutTimeout: Long = 60

    override var clientWindowsTimeout: Long = 60

    override var allowedSelectHiddenView: Boolean = false

    override var lastLayoutDialogPath: String = ""

    override var fileNamePrefix: String = ""

    override fun shouldShowSizeInDp(): Boolean = shouldShowSizeInDp

    override fun showSizeInDp(state: Boolean) {
        shouldShowSizeInDp = state
    }

    override fun shouldStopAdbAfterJob(): Boolean = false
    override fun setStopAdbAfterJob(selected: Boolean) = Unit
}
