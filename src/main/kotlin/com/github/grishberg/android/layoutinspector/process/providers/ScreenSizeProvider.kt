package com.github.grishberg.android.layoutinspector.process.providers

import com.android.ddmlib.*
import java.awt.Dimension
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

private const val SHELL_COMMAND_FOR_SCREEN_SIZE = "dumpsys window"

class ScreenSizeProvider {
    fun getScreenSize(device: IDevice): Dimension {
        val screenSize: String =
            executeShellCommandAndReturnOutput(device, SHELL_COMMAND_FOR_SCREEN_SIZE)
        val size = parseScreenSize(screenSize)
        return Dimension(size[0], size[1])
    }

    private fun parseScreenSize(dumpsisWindow: String): IntArray {
        var width = 0
        var height = 0
        val pattern = "mSystem=\\(\\d*,\\d*\\)-\\((\\d*),(\\d*)\\)"
        // Create a Pattern object
        val r = Pattern.compile(pattern)
        // Now create matcher object.
        val m = r.matcher(dumpsisWindow)
        if (m.find()) {
            width = m.group(1).toInt()
            height = m.group(2).toInt()
        }
        return intArrayOf(width, height)
    }

    private fun executeShellCommandAndReturnOutput(device: IDevice, command: String): String {
        val receiver = CollectingOutputReceiver()
        device.executeShellCommand(command, receiver, 60, TimeUnit.SECONDS)

        return receiver.output
    }
}