package com.github.grishberg.android.layoutinspector.process

import com.android.layoutinspector.model.ViewNode
import com.github.grishberg.android.layoutinspector.domain.DumpViewNode
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test

private const val COMPOSE_FULL_NAME = "androidx.compose.ui.platform.ComposeView"
private const val VIEW_ROOT_FULL_NAME = "com.android.internal.policy.DecorView"
private const val LL_FULL_NAME = "android.widget.LinearLayout"
private const val FL_FULL_NAME = "android.widget.FrameLayout"
private const val TARGET_CHILD_NAME = "TargetChild1"

class TreeMergerTest {

    private val underTest = TreeMerger()

    @Test
    fun mergeSeveralCompose() {
        val viewNodes = createViewNodes()
        val dumpNodes = createDumpNodes()

        val result = underTest.mergeNodes(viewNodes, dumpNodes)

        val root = result
        val llNode = root.children.first()

        assertEquals(LL_FULL_NAME, llNode.name)

        val flNode = llNode.children.first()
        assertEquals(FL_FULL_NAME, flNode.name)
        assertEquals("id/content", flNode.id)

        val flNode2 = flNode.children.first()
        assertEquals(FL_FULL_NAME, flNode2.name)

        val controlsView = flNode2.children[1]
        assertEquals("id/controls", controlsView.id)

        assertTrue(controlsView.children.isNotEmpty())
        val targetChild = controlsView.children.first()
        assertEquals(TARGET_CHILD_NAME, targetChild.name)
    }

    @Test
    fun `merge when dump has no current ComposeView`() {
        val viewNodes = createViewNodes()
        val dumpNodes = createDumpNodesWithWrongPath()

        val result = underTest.mergeNodes(viewNodes, dumpNodes)

        val root = result
        val llNode = root.children.first()

        assertEquals(LL_FULL_NAME, llNode.name)

        val flNode = llNode.children.first()
        assertEquals(FL_FULL_NAME, flNode.name)
        assertEquals("id/content", flNode.id)

        val flNode2 = flNode.children.first()
        assertEquals(FL_FULL_NAME, flNode2.name)

        val controlsView = flNode2.children[1]
        assertEquals("id/controls", controlsView.id)

        assertTrue(controlsView.children.isEmpty())
    }

    private fun createViewNodes(): ViewNode {
        val root = ViewNode(null, VIEW_ROOT_FULL_NAME, "hash_root")

        val llNode = addChild(root, LL_FULL_NAME)

        val flNode = addChild(llNode, FL_FULL_NAME, "id/content")

        val flNode2 = addChild(flNode, FL_FULL_NAME)

        val contentView = addChild(flNode2, "com.github.grishberg.painting.pixels.painting.ZoomableView", "id/content")

        val controlsView = addChild(flNode2, COMPOSE_FULL_NAME, "id/controls")

        return root
    }

    private fun addChild(root: ViewNode, className: String, id: String? = "NO_ID"): ViewNode {
        val child = ViewNode(root, className, "1234")
        child.id = id
        root.children.add(child)
        return child
    }

    private fun createDumpNodes(): DumpViewNode {
        val root = DumpViewNode(null, "", FL_FULL_NAME, null, 0, 0, 0, 0, null)

        val llNode = addChild(root, "android.widget.LinearLayout")

        val flNode = addChild(llNode, FL_FULL_NAME, "id/content")

        val flNode2 = addChild(flNode, FL_FULL_NAME)

        val controlsView = addChild(flNode2, COMPOSE_FULL_NAME, "id/controls")

        val contentView = addChild(flNode2, "android.view.View", "id/content")

        addChild(controlsView, TARGET_CHILD_NAME)

        return root
    }

    private fun createDumpNodesWithWrongPath(): DumpViewNode {
        val root = DumpViewNode(null, "", FL_FULL_NAME, null, 0, 0, 0, 0, null)

        val llNode = addChild(root, LL_FULL_NAME)

        val flNode = addChild(llNode, FL_FULL_NAME, "id/screen")

        val flNode2 = addChild(flNode, FL_FULL_NAME)

        val controlsView = addChild(flNode2, COMPOSE_FULL_NAME, "id/controls")

        val contentView = addChild(flNode2, "android.view.View", "id/content")

        addChild(controlsView, TARGET_CHILD_NAME)

        return root
    }

    private fun addChild(root: DumpViewNode, className: String, id: String? = null): DumpViewNode {
        val child = DumpViewNode(root, "pkg", className, id, 0, 0, 0, 0, null)
        root.addChild(child)
        return child
    }

}
