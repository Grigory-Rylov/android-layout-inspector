package com.github.grishberg.android.layoutinspector.ui

import com.android.layoutinspector.common.AdbFacade
import com.android.layoutinspector.common.AppLogger
import com.github.grishberg.android.layoutinspector.process.providers.DeviceProvider
import com.github.grishberg.android.layoutinspector.settings.SettingsFacade
import java.io.File

/**
 * Manages all Main instances.
 */
class WindowsManager(
    private val logger: AppLogger,
) {
    private val windows = mutableListOf<Main>()

    fun createWindow(
        mode: OpenWindowMode,
        settingsFacade: SettingsFacade,
        deviceProvider: DeviceProvider,
        adb: AdbFacade,
        baseDir: File
    ): Main {
        val main = Main(this, mode, settingsFacade, logger, deviceProvider, adb, baseDir)
        windows.add(main)
        return main
    }

    fun onDestroyed(window: Main) {
        windows.remove(window)
    }

    fun startScreenshotTest(comparableWindow: Main): Boolean {
        if (windows.size != 2) {
            logger.d("startScreenshotTest: there is ${windows.size} windows")
            return false
        }

        val referenceWindow = windows.first { it != comparableWindow }

        val otherWindowScreenshot = comparableWindow.screenshot()
        if (otherWindowScreenshot == null) {
            logger.d("startScreenshotTest: other window has no layout")
            return false
        }

        val referenceScreenshot = referenceWindow.screenshot()
        if (referenceScreenshot == null) {
            logger.d("startScreenshotTest: reference window has no layout")
            return false
        }

        if (otherWindowScreenshot.width != referenceScreenshot.width ||
            otherWindowScreenshot.height != referenceScreenshot.height
        ) {
            logger.d("startScreenshotTest: images size not equals")
            return false
        }
        comparableWindow.screenshotTest(
            referenceScreenshot,
            otherWindowScreenshot,
        )
        return true
    }
}