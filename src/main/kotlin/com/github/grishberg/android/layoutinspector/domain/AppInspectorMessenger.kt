package com.github.grishberg.android.layoutinspector.domain

import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.Command
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.Response

interface AppInspectorMessenger {
    suspend fun sendCommand(command: Command.Builder.() -> Unit): Response
}

interface InspectorClientLaunchMonitor {
    fun updateProgress(state: AttachErrorState)
}

enum class AttachErrorState {
    INITIALIZING,
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    ERROR
}

data class LaunchParameters(
    val process: ProcessDescriptor,
    val inspectorId: String,
    val jar: AppInspectorJar,
    val projectName: String,
    val compatibility: LibraryCompatibility,
    val force: Boolean
)

data class AppInspectorJar(
    val name: String,
    val developmentDirectory: String,
    val releaseDirectory: String
)

interface LibraryCompatibility {
    val minimumVersion: String
}

object COMPOSE_INSPECTION_COMPATIBILITY : LibraryCompatibility {
    override val minimumVersion: String = "1.2.1"
}

interface ProcessDescriptor {
    val name: String
} 