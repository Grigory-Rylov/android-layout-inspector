package com.github.grishberg.androidstudio.plugins

import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType

fun info(msg: String) {
    val noti = NotificationGroup(
        "myplugin",
        NotificationDisplayType.BALLOON, true
    )
    noti.createNotification(
        "My Title",
        "My Message",
        NotificationType.INFORMATION,
        null
    ).notify(null)
}
