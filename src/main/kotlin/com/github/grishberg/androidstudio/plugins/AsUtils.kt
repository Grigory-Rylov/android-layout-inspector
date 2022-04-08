package com.github.grishberg.androidstudio.plugins

import com.android.tools.idea.gradle.project.sync.GradleSyncState
import com.intellij.openapi.project.Project
import org.joor.Reflect

class AsUtils {
    // The android debugger class is not available in Intellij 2016.1.
    // Nobody should use that version but it's still the minimum "supported" version since android studio 2.2
    // shares the same base version.
    val isDebuggingAvailable: Boolean
        get() = try {
            Reflect.on("com.android.tools.idea.run.editor.AndroidDebugger").get<Any>()
            true
        } catch (e: Exception) {
            false
        }

    fun isGradleSyncInProgress(project: Project): Boolean {
        return try {
            GradleSyncState.getInstance(project).isSyncInProgress
        } catch (t: Throwable) {
            info("Couldn't determine if a gradle sync is in progress")
            false
        }
    }
}
