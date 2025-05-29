package com.github.grishberg.android.layoutinspector.domain

import com.android.ddmlib.Client
import com.android.ddmlib.IDevice

data class LayoutRecordOptions(
    var device: IDevice,
    val client: Client,
    val timeoutInSeconds: Int,
    val fileNamePrefix: String,
    val v2Enabled: Boolean,
    val dumpViewModeEnabled: Boolean = false,
    val composeEnabled: Boolean = true,
    val hideSystemNodes: Boolean = true,
    val showRecompositions: Boolean = true,
    val highlightRecompositions: Boolean = true,
    val ignoreRecompositionsInFramework: Boolean = true,
    val label: String? = null,
)
