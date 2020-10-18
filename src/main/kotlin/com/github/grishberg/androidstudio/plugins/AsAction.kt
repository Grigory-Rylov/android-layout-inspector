package com.github.grishberg.androidstudio.plugins

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.Project

abstract class AsAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        actionPerformed(e, e.getData(PlatformDataKeys.PROJECT)!!)
    }

    abstract fun actionPerformed(e: AnActionEvent, project: Project)
}
