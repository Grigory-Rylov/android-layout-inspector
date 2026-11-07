package com.github.grishberg.android.layoutinspector.compose

import com.android.layoutinspector.common.AppLogger
import com.android.layoutinspector.model.DisplayInfo
import com.android.layoutinspector.model.ViewNode
import com.github.grishberg.android.layoutinspector.compose.ComposeInspectionCodec.ComposeNode
import com.github.grishberg.android.layoutinspector.compose.ComposeInspectionCodec.ComposeRect
import com.github.grishberg.android.layoutinspector.compose.ComposeInspectionCodec.ComposableRoot
import com.github.grishberg.android.layoutinspector.domain.AbstractViewNode
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test

/**
 * Grafting tests for [ComposeTreeMerger] with synthetic ViewNode trees:
 * single host fast path, nested host de-duplication, multi-host bounds matching and the
 * "Compose root view <id>" fallback under the decor root.
 */
class ComposeTreeMergerTest {

    private val underTest = ComposeTreeMerger(LoggerStub())

    @Test
    fun `single compose host gets adapted children with screen coordinates`() {
        // decor(0,0) -> linear(0,0) -> ComposeView(100,200 size 800x600)
        val decor = viewNode(null, DECOR, 0, 0, 1080, 1920)
        val linear = viewNode(decor, LINEAR, 0, 0, 1080, 1920)
        val composeView = viewNode(linear, COMPOSE_VIEW, 100, 200, 800, 600)

        // node bounds are ABSOLUTE screen pixels (agent adds the window root view's location)
        val child = composeNode(1, "Text", "MainActivity.kt", 42, ComposeRect(110, 220, 100, 50), viewId = 7)
        val top = composeNode(2, "Column", "MainActivity.kt", 20, ComposeRect(100, 200, 800, 600), listOf(child), viewId = 7)
        val root = ComposableRoot(viewId = 7, nodes = listOf(top), viewsToSkip = emptyList())

        underTest.mergeNodes(decor, listOf(root))

        assertEquals(1, composeView.children.size)
        val grafted = composeView.children.first() as ViewNode
        assertEquals("MainActivity.kt:20:Column", grafted.name)
        // absolute compose bounds are kept as-is
        assertEquals(100, grafted.locationOnScreenX)
        assertEquals(200, grafted.locationOnScreenY)
        assertEquals(800, grafted.width)

        assertEquals(1, grafted.children.size)
        val graftedChild = grafted.children.first()
        assertEquals("MainActivity.kt:42:Text", graftedChild.name)
        assertEquals(110, graftedChild.locationOnScreenX)
        assertEquals(220, graftedChild.locationOnScreenY)

        // ViewProperty rows carry compose metadata
        assertEquals("7", grafted.namedProperties["compose:view_id"]?.value)
        assertEquals("2", grafted.namedProperties["compose:id"]?.value)
        assertEquals("MainActivity.kt", grafted.namedProperties["compose:file"]?.value)
        assertEquals("20", grafted.namedProperties["compose:line_number"]?.value)
        assertEquals("[100,200][900,800]", grafted.namedProperties["compose:bounds"]?.value)
    }

    @Test
    fun `nested compose host keeps only the innermost graft target`() {
        val decor = viewNode(null, DECOR, 0, 0, 1080, 1920)
        val composeView = viewNode(decor, COMPOSE_VIEW, 0, 100, 1080, 900)
        val androidComposeView = viewNode(composeView, ANDROID_COMPOSE_VIEW, 0, 0, 1080, 900)

        val root = ComposableRoot(
            viewId = 1,
            nodes = listOf(composeNode(1, "App", "App.kt", 5, ComposeRect(0, 0, 1080, 900))),
            viewsToSkip = emptyList(),
        )
        underTest.mergeNodes(decor, listOf(root))

        assertEquals("graft must land on the innermost host", 1, androidComposeView.children.size)
        assertEquals("App.kt:5:App", androidComposeView.children.first().name)
        assertEquals("outer ComposeView must keep only its own child", listOf(androidComposeView), composeView.children)
        assertEquals("only ComposeView under decor", 1, decor.children.size)
    }

    @Test
    fun `multi root multi host matches by bounds`() {
        val decor = viewNode(null, DECOR, 0, 0, 1080, 1920)
        // two independent hosts far apart on screen
        val hostA = viewNode(decor, ANDROID_COMPOSE_VIEW, 0, 0, 1080, 900)
        val hostB = viewNode(decor, ANDROID_COMPOSE_VIEW, 0, 1000, 400, 300)

        // absolute bounds overlapping their host's absolute rect
        val rootSmall = ComposableRoot(
            viewId = 11,
            nodes = listOf(composeNode(1, "Small", "A.kt", 1, ComposeRect(0, 1000, 400, 300))),
            viewsToSkip = emptyList(),
        )
        val rootBig = ComposableRoot(
            viewId = 12,
            nodes = listOf(composeNode(2, "Big", "B.kt", 1, ComposeRect(0, 0, 1080, 900))),
            viewsToSkip = emptyList(),
        )

        underTest.mergeNodes(decor, listOf(rootSmall, rootBig))

        assertEquals(listOf("A.kt:1:Small"), hostB.children.map { it.name })
        assertEquals(listOf("B.kt:1:Big"), hostA.children.map { it.name })
        assertEquals(0, decor.children.filter { it.name.startsWith("Compose root view") }.size)
    }

    @Test
    fun `leftover root without host is appended under decor`() {
        val decor = viewNode(null, DECOR, 0, 0, 1080, 1920)
        viewNode(decor, LINEAR, 0, 0, 1080, 1920) // no compose host at all

        val root = ComposableRoot(
            viewId = 33,
            nodes = listOf(composeNode(1, "Detached", "X.kt", 9, ComposeRect(0, 64, 1080, 1856))),
            viewsToSkip = emptyList(),
        )
        underTest.mergeNodes(decor, listOf(root))

        val synthetic = decor.children.last()
        assertEquals("Compose root view 33", synthetic.name)
        assertEquals(1, synthetic.children.size)
        assertEquals("X.kt:9:Detached", synthetic.children.first().name)
        // decor is at (0,0); compose window offset (0,64) -> absolute (0,64)
        assertEquals(0, synthetic.locationOnScreenX)
        assertEquals(64, synthetic.locationOnScreenY)
        assertEquals(1, decor.children.count { it !== synthetic && it.name == LINEAR })
    }

    @Test
    fun `empty roots leave the tree untouched`() {
        val decor = viewNode(null, DECOR, 0, 0, 1080, 1920)
        val host = viewNode(decor, ANDROID_COMPOSE_VIEW, 0, 0, 100, 100)
        underTest.mergeNodes(decor, emptyList())
        assertEquals(0, host.children.size)
        assertEquals(1, decor.children.size)
    }

    // ==== fixtures ====

    private fun viewNode(
        parent: ViewNode?,
        name: String,
        left: Int,
        top: Int,
        width: Int,
        height: Int,
    ): ViewNode {
        val node = ViewNode(
            parent = parent,
            name = name,
            hash = "hash-$name-$left-$top",
            displayInfo = DisplayInfo(
                false, true, left, top, width, height, 0, 0, true, 0f, 0f, 0f, 0f, null,
            ),
        )
        if (parent != null) {
            // AbstractViewNode.children is typed read-only; the concrete storage is mutable
            @Suppress("UNCHECKED_CAST")
            (parent.children as MutableList<AbstractViewNode>).add(node)
        }
        return node
    }

    private fun composeNode(
        id: Long,
        name: String,
        file: String,
        line: Int,
        bounds: ComposeRect,
        children: List<ComposeNode> = emptyList(),
        viewId: Long = 0,
    ) = ComposeNode(
        id = id,
        name = name,
        file = file,
        line = line,
        offset = 0,
        bounds = bounds,
        flags = 0,
        viewId = viewId,
        recomposeCount = 0,
        recomposeSkips = 0,
        packageHash = 0,
        anchorHash = 0,
        children = children,
    )

    private class LoggerStub : AppLogger {
        override fun d(msg: String) = Unit
        override fun e(msg: String) = Unit
        override fun e(msg: String, t: Throwable) = Unit
        override fun w(msg: String) = Unit
    }
}

private const val DECOR = "com.android.internal.policy.DecorView"
private const val LINEAR = "android.widget.LinearLayout"
private const val COMPOSE_VIEW = "androidx.compose.ui.platform.ComposeView"
private const val ANDROID_COMPOSE_VIEW = "androidx.compose.ui.platform.AndroidComposeView"
