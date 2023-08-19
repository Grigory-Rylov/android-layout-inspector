package com.github.grishberg.android.li.ui

import com.github.grishberg.androidstudio.plugins.NotificationHelper
import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

class NotificationHelperImpl(private val project: Project) : NotificationHelper {

    override fun info(message: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Notification Group")
            .createNotification(escapeString(message), NotificationType.INFORMATION)
            .notify(project)
    }

    override fun info(title: String, message: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Notification Group")
            .createNotification(title, escapeString(message), NotificationType.INFORMATION)
            .notify(project)
    }

    override fun supportInfo(title: String, message: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Support YALI Notification")
            .createNotification(title, escapeString(message), NotificationType.INFORMATION)
            .notify(project)
    }

    override fun error(message: String)  {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Error Notification Group")
            .createNotification(escapeString(message), NotificationType.ERROR)
            .notify(project)
    }

    private fun escapeString(string: String) = string.replace("\n".toRegex(), "\n<br />")
}
