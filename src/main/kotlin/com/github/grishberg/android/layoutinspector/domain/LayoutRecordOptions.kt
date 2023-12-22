package com.github.grishberg.android.layoutinspector.domain

import com.android.ddmlib.Client
import com.android.ddmlib.IDevice

data class LayoutRecordOptions(
    var device: IDevice,
    val client: Client,
    val timeoutInSeconds: Int,
    val fileNamePrefix: String,
    val v2Enabled: Boolean,
    val recordingMode: RecordingMode,
)

fun RecordingMode.hasLayouts(): Boolean {
    return this == RecordingMode.Layouts || this == RecordingMode.LayoutsAndDump ||
        this == RecordingMode.LayoutsAndComposeDump
}

fun RecordingMode.hasHierarchyDump(): Boolean {
    return this == RecordingMode.Dump || this == RecordingMode.LayoutsAndDump ||
        this == RecordingMode.LayoutsAndComposeDump
}
