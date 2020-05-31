package com.github.grishberg.android.layoutinspector.domain

import com.android.layoutinspector.model.ClientWindow

interface ClientWindowsInput {
    suspend fun getSelectedWindow(options: LayoutRecordOptions): ClientWindow
}
