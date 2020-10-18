package com.github.grishberg.android.li.ui

import com.github.grishberg.androidstudio.plugins.ConnectedDeviceInfo
import java.lang.StringBuilder
import javax.swing.JFrame
import javax.swing.JLabel


class MainFrame(deviceInfo: ConnectedDeviceInfo?) {
    init {
        val frame = JFrame()
        frame.title = "Welecome to JavaTutorial.net"
        frame.setSize(600, 400)
        //frame.setLocation(200, 200)
        frame.setLocationRelativeTo(null)
        val label1 = JLabel()

        frame.add(label1)
        if (deviceInfo == null) {
            label1.text = "adb not connected"
        } else {
            label1.text = "adb connected, pkg: ${deviceInfo.packageName}, devices: ${deviceInfo.devices.size}"

            val device = deviceInfo.devices.first()
            val clients = device.clients
            val sb = StringBuilder()
            for (c in clients) {
                sb.append(c.clientData?.packageName?:"-")
                sb.append("\n")
            }
            label1.text = sb.toString()
        }

        frame.isVisible = true
    }
}
