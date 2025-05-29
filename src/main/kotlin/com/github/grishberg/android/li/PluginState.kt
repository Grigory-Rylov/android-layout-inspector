package com.github.grishberg.android.li

import com.github.grishberg.android.layoutinspector.settings.SettingsFacade

class PluginState : SettingsFacade {
    var shouldShowSizeInDp = false

    override var captureLayoutTimeout: Long = 60

    override var clientWindowsTimeout: Long = 60

    override var allowedSelectHiddenView: Boolean = false

    override var lastLayoutDialogPath: String = ""

    override var fileNamePrefix: String = ""

    override var ignoreLastClickedView: Boolean = true

    override var roundDimensions: Boolean = true

    override var lastProcessName: String = ""

    override var lastWindowName: String = ""

    override var lastFilter: String = ""

    override var showSerifsInTheMiddleOfSelected: Boolean = true

    override var showSerifsInTheMiddleAll: Boolean = false

    override var isSecondProtocolVersionEnabled: Boolean = false

    override var lastVersion: String = ""

    override var isDumpViewModeEnabled: Boolean = false

    private var composeEnabled: Boolean = true
    private var hideSystemNodes: Boolean = true
    private var showRecompositions: Boolean = true
    private var highlightRecompositions: Boolean = true
    private var ignoreRecompositionsInFramework: Boolean = true

    override fun shouldShowSizeInDp(): Boolean = shouldShowSizeInDp

    override fun showSizeInDp(state: Boolean) {
        shouldShowSizeInDp = state
    }

    override fun shouldStopAdbAfterJob(): Boolean = false

    override fun setStopAdbAfterJob(selected: Boolean) = Unit

    override fun setSizeDpMode(enabled: Boolean) {
        shouldShowSizeInDp = enabled
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
