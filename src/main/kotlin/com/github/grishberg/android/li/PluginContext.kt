package com.github.grishberg.android.li

import com.github.grishberg.androidstudio.plugins.AdbWrapperImpl
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.project.Project

class PluginContext(private val project: Project) : ProjectComponent {
    val adb by lazy { AdbWrapperImpl(project) }
}

fun Project.context(): PluginContext {
    return this.getComponent(PluginContext::class.java)
}
