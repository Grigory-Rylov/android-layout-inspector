package com.github.grishberg.androidstudio.plugins

import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType

fun info(msg: String) {
    val noti = NotificationGroup(
        "YALI",
        NotificationDisplayType.BALLOON, true
    )
    noti.createNotification(
        "Info:",
        msg,
        NotificationType.INFORMATION,
        null
    ).notify(null)
}
