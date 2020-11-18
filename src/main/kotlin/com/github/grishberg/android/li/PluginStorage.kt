package com.github.grishberg.android.li

import com.github.grishberg.android.layoutinspector.settings.SettingsFacade
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State

@State()
class PluginStorage : PersistentStateComponent<SettingsFacade> {
    private var storage: SettingsFacade = Storage()

    override fun getState(): SettingsFacade = storage

    override fun loadState(newStorage: SettingsFacade) {
        storage = newStorage
    }

    private class Storage : SettingsFacade {
        private var shouldShowSizeInDp = false

        override var captureLayoutTimeout: Long = 60

        override var clientWindowsTimeout: Long = 60

        override var allowedSelectHiddenView: Boolean = false

        override var lastLayoutDialogPath: String = ""

        override var fileNamePrefix: String = ""

        override fun shouldShowSizeInDp(): Boolean = shouldShowSizeInDp

        override fun showSizeInDp(state: Boolean) {
            shouldShowSizeInDp = state
        }
    }
}