package com.github.grishberg.android.layoutinspector

import com.android.layoutinspector.LayoutInspectorCaptureOptions
import com.android.layoutinspector.model.DisplayInfo
import com.android.layoutinspector.model.LayoutFileData
import com.android.layoutinspector.model.ViewNode

object Utils {
    fun createSample1(): LayoutFileData {
        val rootNode: ViewNode = createViewNode()
        return LayoutFileData(null, rootNode, LayoutInspectorCaptureOptions(), 1.0)
    }

    fun createViewNode(): ViewNode {
        val di = createVisibleDisplayInfo(true, 0, 0, 100, 100)
        val root = createViewNode(null, "root", di)

        val child1 = createViewNode(root, "child1", createVisibleDisplayInfo(10, 10, 80, 80))
        createViewNode(child1, "child2", createVisibleDisplayInfo(10, 10, 60, 30))
        createViewNode(child1, "child3", createVisibleDisplayInfo(10, 42, 60, 30))
        return root
    }

    fun createViewNode(parent: ViewNode?, name: String, displayInfo: DisplayInfo): ViewNode {
        val node = ViewNode(parent, name, "${name.hashCode()}")
        node.displayInfo = displayInfo
        parent?.let {
            it.children.add(node)
        }
        return node
    }

    fun createVisibleDisplayInfo(
        left: Int,
        top: Int,
        width: Int,
        height: Int
    ): DisplayInfo = createVisibleDisplayInfo(true, left, top, width, height)


    fun createVisibleDisplayInfo(
        isVisible: Boolean,
        left: Int,
        top: Int,
        width: Int,
        height: Int
    ): DisplayInfo {
        return DisplayInfo(
            false, isVisible,
            left, top, width, height, 0, 0, false, 0f, 0f,
            1f, 1f, ""
        )
    }
}