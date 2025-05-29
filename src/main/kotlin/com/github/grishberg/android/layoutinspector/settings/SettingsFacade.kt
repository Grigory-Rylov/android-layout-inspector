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

    var isDumpViewModeEnabled: Boolean

    fun shouldShowSizeInDp(): Boolean

    fun showSizeInDp(state: Boolean)

    fun shouldStopAdbAfterJob(): Boolean

    fun setStopAdbAfterJob(selected: Boolean)

    var lastVersion: String

    fun setSizeDpMode(enabled: Boolean)

    fun isComposeEnabled(): Boolean

    fun setComposeEnabled(enabled: Boolean)

    fun getHideSystemNodes(): Boolean

    fun setHideSystemNodes(enabled: Boolean)

    fun getShowRecompositions(): Boolean

    fun setShowRecompositions(enabled: Boolean)

    fun getHighlightRecompositions(): Boolean

    fun setHighlightRecompositions(enabled: Boolean)

    fun getIgnoreRecompositionsInFramework(): Boolean

    fun setIgnoreRecompositionsInFramework(enabled: Boolean)
}

class SettingsFacadeImpl : SettingsFacade {
    private var showSizeInDp = false
    private var composeEnabled = true
    private var hideSystemNodes = true
    private var showRecompositions = true
    private var highlightRecompositions = true
    private var ignoreRecompositionsInFramework = true

    override fun shouldShowSizeInDp(): Boolean = showSizeInDp

    override fun showSizeInDp(state: Boolean) {
        showSizeInDp = state
    }

    override fun shouldStopAdbAfterJob(): Boolean = false

    override fun setStopAdbAfterJob(selected: Boolean) {
        // Implementation needed
    }

    override fun setSizeDpMode(enabled: Boolean) {
        showSizeInDp = enabled
    }

    override fun isComposeEnabled(): Boolean = composeEnabled

    override fun setComposeEnabled(enabled: Boolean) {
        composeEnabled = enabled
    }

    override fun getHideSystemNodes(): Boolean = hideSystemNodes

    override fun setHideSystemNodes(enabled: Boolean) {
        hideSystemNodes = enabled
    }

    override fun getShowRecompositions(): Boolean = showRecompositions

    override fun setShowRecompositions(enabled: Boolean) {
        showRecompositions = enabled
    }

    override fun getHighlightRecompositions(): Boolean = highlightRecompositions

    override fun setHighlightRecompositions(enabled: Boolean) {
        highlightRecompositions = enabled
    }

    override fun getIgnoreRecompositionsInFramework(): Boolean = ignoreRecompositionsInFramework

    override fun setIgnoreRecompositionsInFramework(enabled: Boolean) {
        ignoreRecompositionsInFramework = enabled
    }
}
