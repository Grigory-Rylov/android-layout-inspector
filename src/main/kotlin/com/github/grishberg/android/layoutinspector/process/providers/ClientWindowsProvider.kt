package com.github.grishberg.android.layoutinspector.process.providers

import com.android.layoutinspector.common.AppLogger
import com.android.layoutinspector.model.ClientWindow
import com.github.grishberg.android.layoutinspector.domain.ClientWindowsInput
import com.github.grishberg.android.layoutinspector.domain.LayoutRecordOptions
import java.util.concurrent.TimeUnit

class ClientWindowsProvider(
    private val logger: AppLogger
) : ClientWindowsInput {
    override suspend fun getClientWindows(options: LayoutRecordOptions): List<ClientWindow> {
        return ClientWindow.getAll(
            logger,
            options.client,
            options.timeoutInSeconds.toLong(),
            TimeUnit.SECONDS
        ) ?: emptyList()
    }
}
