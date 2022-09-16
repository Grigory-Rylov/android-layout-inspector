package com.github.grishberg.android.layoutinspector.settings

interface SettingsFacade {
    var captureLayoutTimeout: Long

    var clientWindowsTimeout: Long

    var allowedSelectHiddenView: Boolean

    var lastLayoutDialogPath: String

    var fileNamePrefix: String

    var ignoreLastClickedView: Boolean

    var roundDimensions: Boolean

    var lastProcessName: String

    var lastWindowName: String

    var lastFilter: String

    var showSerifsInTheMiddleOfSelected: Boolean

    var showSerifsInTheMiddleAll: Boolean

    var isSecondProtocolVersionEnabled: Boolean

    fun shouldShowSizeInDp(): Boolean

    fun showSizeInDp(state: Boolean)

    fun shouldStopAdbAfterJob(): Boolean

    fun setStopAdbAfterJob(selected: Boolean)
}
