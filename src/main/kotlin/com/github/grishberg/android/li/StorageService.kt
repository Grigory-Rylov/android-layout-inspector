package com.github.grishberg.android.li

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(
    name = "PluginSettingsState",
    storages = [Storage("yali-settings.xml")]
)
class StorageService : PersistentStateComponent<PluginState> {
    private var storage = PluginState()

    override fun getState(): PluginState = storage

    override fun loadState(newStorage: PluginState) {
        storage = newStorage
    }

    companion object {
        @JvmStatic
        fun getInstance(): PersistentStateComponent<PluginState> {
            return ApplicationManager.getApplication().getService(StorageService::class.java)
        }
    }
}
