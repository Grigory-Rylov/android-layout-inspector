package com.github.grishberg.android.layoutinspector.domain

import com.github.grishberg.android.layoutinspector.settings.TreeSettings
import com.github.grishberg.android.layoutinspector.ui.InspectorModel
import com.github.grishberg.android.layoutinspector.ui.NotificationModel
import com.github.grishberg.android.layoutinspector.ui.Status
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.*
import java.util.*

/**
 * The client responsible for interacting with the compose layout inspector running on the target
 * device.
 *
 * @param messenger The messenger that lets us communicate with the view inspector.
 * @param capabilities Of the containing [InspectorClient]. Some capabilities may be added by this
 *   class.
 */
class ComposeLayoutInspectorClient(
    private val model: InspectorModel,
    private val treeSettings: TreeSettings,
    private val messenger: AppInspectorMessenger,
    private val capabilities: EnumSet<Capability>,
    private val launchMonitor: InspectorClientLaunchMonitor,
    val composeVersion: String?,
) {
    private var pendingRecompositionCountReset = false

    suspend fun getComposables(
        rootViewId: Long,
        skipSystemComposables: Boolean,
        generation: Int,
        extractAllParameters: Boolean,
    ): GetComposablesResult {
        val command = Command.newBuilder().apply {
            getComposablesBuilder.apply {
                this.rootViewId = rootViewId
                this.skipSystemComposables = skipSystemComposables
                this.generation = generation
                this.extractAllParameters = extractAllParameters
            }
        }

        val response = messenger.sendCommand { command.build() }
        val result = response.getComposables

        return GetComposablesResult(result, pendingRecompositionCountReset)
    }

    suspend fun updateSettings() {
        val command = Command.newBuilder().apply {
            updateSettingsBuilder.apply {
                delayParameterExtraction = true
                resetRecompositionCounts = false
                highlightRecompositions = treeSettings.highlightRecompositions
            }
        }

        messenger.sendCommand { command.build() }
    }

    suspend fun resetRecompositionCounts() {
        val command = Command.newBuilder().apply {
            updateSettingsBuilder.apply {
                delayParameterExtraction = true
                resetRecompositionCounts = true
                highlightRecompositions = treeSettings.highlightRecompositions
            }
        }

        messenger.sendCommand { command.build() }
        pendingRecompositionCountReset = true
    }

    companion object {
        suspend fun launch(
            apiServices: AppInspectionApiServices,
            process: ProcessDescriptor,
            model: InspectorModel,
            notificationModel: NotificationModel,
            treeSettings: TreeSettings,
            capabilities: EnumSet<Capability>,
            launchMonitor: InspectorClientLaunchMonitor,
            logErrorToMetrics: (AttachErrorCode) -> Unit,
            isRunningFromSourcesInTests: Boolean? = null, // Should only be set from tests
        ): ComposeLayoutInspectorClient? {
            val jar = AppInspectorJar(
                "compose-ui-inspection.jar",
                developmentDirectory = "bazel-bin/tools/base/dynamic-layout-inspector/agent/appinspection/",
                releaseDirectory = "plugins/android/resources/app-inspection/",
            )

            val params = LaunchParameters(
                process,
                COMPOSE_LAYOUT_INSPECTOR_ID,
                jar,
                model.project.name,
                COMPOSE_INSPECTION_COMPATIBILITY,
                force = true,
            )

            return try {
                val messenger = apiServices.launchInspector(params)
                val client = ComposeLayoutInspectorClient(
                    model,
                    treeSettings,
                    messenger,
                    capabilities,
                    launchMonitor,
                    composeVersion,
                ).apply { updateSettings() }

                client
            } catch (e: Exception) {
                notificationModel.addNotification(
                    "compose.inspection.error",
                    "Failed to launch Compose inspector: ${e.message}",
                    Status.Error
                )
                null
            }
        }

        private const val COMPOSE_LAYOUT_INSPECTOR_ID = "layoutinspector.compose.inspection"
    }
}

interface AppInspectorMessenger {
    suspend fun sendCommand(command: Command.Builder.() -> Unit): Response
}

interface InspectorClientLaunchMonitor {
    fun updateProgress(state: AttachErrorState)
}

enum class AttachErrorCode {
    UNKNOWN,
    NO_COMPOSE,
    INCOMPATIBLE_VERSION,
    LAUNCH_FAILED
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