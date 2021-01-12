package com.github.grishberg.android.layoutinspector.domain

import com.android.layoutinspector.model.ClientWindow

/**
 * Returns client windows from device.
 */
interface ClientWindowsInput {
    suspend fun getClientWindows(options: LayoutRecordOptions): List<ClientWindow>
}
