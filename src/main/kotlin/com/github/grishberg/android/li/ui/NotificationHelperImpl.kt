package com.github.grishberg.android.li.ui

import com.github.grishberg.androidstudio.plugins.NotificationHelper
import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType

object NotificationHelperImpl : NotificationHelper {
    private val INFO = NotificationGroup("YALI (Logging)", NotificationDisplayType.NONE, true, null, null)
    private val ERRORS = NotificationGroup("YALI (Errors)", NotificationDisplayType.BALLOON, true, null, null)

    override fun info(message: String) = sendNotification(message, NotificationType.INFORMATION, INFO)

    override fun error(message: String) = sendNotification(message, NotificationType.ERROR, ERRORS)

    private fun sendNotification(
        message: String,
        notificationType: NotificationType,
        notificationGroup: NotificationGroup
    ) {
        notificationGroup.createNotification("YALI", escapeString(message), notificationType, null).notify(null)
    }

    private fun escapeString(string: String) = string.replace("\n".toRegex(), "\n<br />")
}
