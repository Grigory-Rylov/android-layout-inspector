package com.github.grishberg.androidstudio.plugins

interface NotificationHelper {
    fun info(message: String)
    fun info(title: String, message: String)
    fun supportInfo(title: String, message: String)

    fun error(message: String)
}
