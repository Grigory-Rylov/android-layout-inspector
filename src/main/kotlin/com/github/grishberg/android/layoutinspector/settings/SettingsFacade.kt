package com.github.grishberg.android.layoutinspector.settings

interface SettingsFacade {
    var captureLayoutTimeout: Long

    var clientWindowsTimeout: Long

    var allowedSelectHiddenView: Boolean

    var lastLayoutDialogPath: String

    var fileNamePrefix: String

    var ignoreLastClickedView: Boolean

    var lastProcessName: String

    var lastWindowName: String

    fun shouldShowSizeInDp(): Boolean

    fun showSizeInDp(state: Boolean)

    fun shouldStopAdbAfterJob(): Boolean

    fun setStopAdbAfterJob(selected: Boolean)
}
