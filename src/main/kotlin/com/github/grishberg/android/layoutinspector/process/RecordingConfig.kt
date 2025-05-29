package com.github.grishberg.android.layoutinspector.process

import com.android.ddmlib.Client
import com.android.ddmlib.IDevice
import com.android.layoutinspector.model.ClientWindow
import com.github.grishberg.android.layoutinspector.domain.LayoutRecordOptions

data class RecordingConfig(
    val client: Client,
    val clientWindow: ClientWindow,
    val timeoutInSeconds: Int,
    val v2Enabled: Boolean,
    val recordOptions: LayoutRecordOptions,
    val composeEnabled: Boolean = true
) {
    val device: IDevice
        get() = client.device
}
