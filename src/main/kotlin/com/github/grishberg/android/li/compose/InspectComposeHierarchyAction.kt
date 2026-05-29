package com.github.grishberg.android.li.compose

import com.android.ddmlib.CollectingOutputReceiver
import com.android.ddmlib.IDevice
import com.android.layoutinspector.common.PluginLogger
import com.github.grishberg.android.li.ui.NotificationHelperImpl
import com.github.grishberg.androidstudio.plugins.AdbProvider
import com.github.grishberg.androidstudio.plugins.AdbWrapper
import com.github.grishberg.androidstudio.plugins.AdbWrapperImpl
import com.github.grishberg.androidstudio.plugins.AsAction
import com.github.grishberg.androidstudio.plugins.ConnectedDeviceInfoProvider
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import java.io.BufferedReader
import java.io.File
import java.util.concurrent.TimeUnit

private const val TAG = "InspectComposeHierarchyAction"
private const val UI_DUMP_PATTERN = "UI\\shierchary\\sdumped\\sto:\\s([^ ]+\\.xml)"

class InspectComposeHierarchyAction : AsAction() {

    private val logger = PluginLogger()

    override fun actionPerformed(e: AnActionEvent, project: Project) {
        val notifications = NotificationHelperImpl(project)
        val adbProvider = object : AdbProvider {
            override fun getAdb(): AdbWrapper = AdbWrapperImpl(project)
        }
        val provider = ConnectedDeviceInfoProvider(adbProvider, notifications)
        val info = provider.provideDeviceInfo() ?: return

        if (info.devices.isEmpty()) {
            notifications.error("No connected devices")
            return
        }

        val device = chooseDevice(project, info.devices) ?: return

        object : Task.Backgroundable(project, "Capturing Compose hierarchy from ${device.serialNumber}", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true

                val xml = try {
                    captureUiAutomatorDump(device, indicator)
                } catch (ex: Exception) {
                    logger.e("$TAG: dump failed", ex)
                    showError(notifications, "Failed to capture UI dump: ${ex.message}")
                    return
                }

                if (xml.isNullOrBlank()) {
                    showError(notifications, "UI Automator returned an empty dump")
                    return
                }

                indicator.text = "Analysing dump..."
                val result = try {
                    ComposeHierarchyAnalyzer().analyze(device.toString(), xml)
                } catch (ex: Exception) {
                    logger.e("$TAG: analysis failed", ex)
                    showError(notifications, "Failed to analyse dump: ${ex.message}")
                    return
                }

                ApplicationManager.getApplication().invokeLater {
                    ComposeInspectDialog(project, result.summary, result.root).show()
                }
            }
        }.queue()
    }

    private fun chooseDevice(project: Project, devices: List<IDevice>): IDevice? {
        if (devices.size == 1) return devices.first()
        val labels = devices.map { describe(it) }.toTypedArray()
        val selectedIdx = Messages.showChooseDialog(
            project,
            "Select a device to inspect:",
            "Inspect Compose Hierarchy",
            null,
            labels,
            labels.firstOrNull() ?: ""
        )
        if (selectedIdx < 0) return null
        return devices[selectedIdx]
    }

    private fun describe(device: IDevice): String {
        val parts = mutableListOf(device.serialNumber)
        val model = runCatching { device.getProperty("ro.product.model") }.getOrNull()
        if (!model.isNullOrEmpty()) parts.add(model)
        val release = runCatching { device.getProperty("ro.build.version.release") }.getOrNull()
        if (!release.isNullOrEmpty()) parts.add("Android $release")
        return parts.joinToString(" / ")
    }

    private fun captureUiAutomatorDump(device: IDevice, indicator: ProgressIndicator): String? {
        indicator.text = "Running uiautomator dump..."
        val receiver = CollectingOutputReceiver()
        device.executeShellCommand("uiautomator dump", receiver)
        receiver.awaitCompletion(60L, TimeUnit.SECONDS)
        val output = receiver.output ?: return null
        logger.d("$TAG: uiautomator output: $output")
        val match = UI_DUMP_PATTERN.toRegex().find(output) ?: return null
        val remotePath = match.groupValues[1]

        indicator.text = "Pulling dump file..."
        val tempFile = File.createTempFile("yali_compose_dump_", ".xml")
        tempFile.deleteOnExit()
        device.pullFile(remotePath, tempFile.absolutePath)

        val text = tempFile.bufferedReader().use(BufferedReader::readText)
        logger.d("$TAG: dump size=${text.length}")
        return text
    }

    private fun showError(notifications: NotificationHelperImpl, message: String) {
        ApplicationManager.getApplication().invokeLater {
            notifications.error(message)
        }
    }
}
