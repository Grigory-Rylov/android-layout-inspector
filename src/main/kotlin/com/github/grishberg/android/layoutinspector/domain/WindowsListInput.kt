package com.github.grishberg.android.layoutinspector.domain

import com.android.layoutinspector.model.ClientWindow

/**
 * Allows to select window from list.
 */
interface WindowsListInput {
    suspend fun getSelectedWindow(windows: List<ClientWindow>): ClientWindow
}
