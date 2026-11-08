package com.github.grishberg.android.layoutinspector.compose

import com.android.layoutinspector.model.DisplayInfo
import com.android.layoutinspector.model.ViewNode
import com.android.layoutinspector.model.ViewProperty
import com.github.grishberg.android.layoutinspector.compose.ComposeInspectionCodec.ComposeNode
import com.github.grishberg.android.layoutinspector.domain.AbstractViewNode

/**
 * Converts decoded Compose hierarchy nodes ([ComposeNode]) into YALI [ViewNode]s so they appear
 * in the tree/properties UI like regular view nodes.
 *
 * Coordinate convention (verified against the on-device compose agent 1.11.3,
 * ComposeExtensionsKt.toFlatNode): Compose node `bounds.layout` x/y are **absolute screen
 * pixels** — the agent already adds the window root view's `getLocationOnScreen()` to each
 * node's compose-local coordinates before serializing. So:
 *  - `layout:getLocationOnScreen_x()/y()` properties take the node bounds as-is;
 *  - [DisplayInfo.left]/[DisplayInfo.top] carry the offset relative to the YALI parent node
 *    (ViewNode combines them with the parent's absolute position), consistent with how the JDWP
 *    and uiautomator-merged nodes behave.
 *
 * NOTE: if the graft parent is not a [ViewNode] (e.g. a DumpViewNode from uiautomator-merge mode),
 * the node is created with a null typed parent; absolute coordinates still work because ViewNode
 * prefers the location properties over parent accumulation.
 */
object ComposeNodeAdapter {

    private const val CATEGORY = "compose"
    private const val LAYOUT_CATEGORY = "layout"

    // ComposableNode.Flags from compose_layout_inspection.proto
    private val FLAG_NAMES = linkedMapOf(
        0x1 to "SYSTEM_CREATED",
        0x2 to "HAS_MERGED_SEMANTICS",
        0x4 to "HAS_UNMERGED_SEMANTICS",
        0x8 to "INLINED",
        0x10 to "NESTED_SINGLE_CHILDREN",
        0x20 to "HAS_DRAW_MODIFIER",
        0x40 to "HAS_CHILD_DRAW_MODIFIER",
    )

    /**
     * Adapts the top-level [nodes] of one ComposableRoot; [hostYaliNode] is the YALI node the
     * compose trees are grafted under (only used as the typed parent — bounds are absolute).
     */
    fun adaptRootNodes(
        nodes: List<ComposeNode>,
        hostYaliNode: AbstractViewNode,
        semanticTexts: Map<Long, String> = emptyMap(),
    ): List<ViewNode> = nodes.map { adaptNode(it, hostYaliNode, semanticTexts) }

    private fun adaptNode(
        node: ComposeNode,
        yaliParent: AbstractViewNode,
        semanticTexts: Map<Long, String>,
    ): ViewNode {
        val absX = node.bounds.left
        val absY = node.bounds.top
        val parentAbsX = yaliParent.locationOnScreenX
        val parentAbsY = yaliParent.locationOnScreenY

        val text = semanticTexts[node.id]
        val properties = buildProperties(node, absX, absY, text)
        val viewNode = ViewNode(
            parent = yaliParent as? ViewNode,
            name = displayName(node, text),
            hash = "compose-${node.id}",
            namedProperties = properties.associateBy { it.fullName },
            properties = properties,
            children = mutableListOf(),
            displayInfo = DisplayInfo(
                /* willNotDraw = */ false,
                /* isVisible = */ true,
                /* left = */ absX - parentAbsX,
                /* top = */ absY - parentAbsY,
                /* width = */ node.bounds.width,
                /* height = */ node.bounds.height,
                /* scrollX = */ 0,
                /* scrollY = */ 0,
                /* clipChildren = */ true,
                /* translateX = */ 0f,
                /* translateY = */ 0f,
                /* scaleX = */ 0f,
                /* scaleY = */ 0f,
                /* contentDesc = */ null,
            ),
        )
        viewNode.children.addAll(node.children.map { adaptNode(it, viewNode, semanticTexts) })
        return viewNode
    }

    /**
     * Tree label: `<fileShort>.kt:<line>:<composableName>`, plus the rendered text in quotes
     * when the semantics carried one (the compose agent may report full paths in [ComposeNode.file]).
     */
    fun displayName(node: ComposeNode, text: String? = null): String {
        val fileShort = node.file
            .substringAfterLast('/')
            .removeSuffix(".kt")
            .ifEmpty { "unknown" }
        val label = "$fileShort.kt:${node.line}:${node.name}"
        return if (text == null) label else "$label(\"${displayText(text)}\")"
    }

    /** One line, bounded: tree rows must stay readable for long AnnotatedStrings. */
    private fun displayText(text: String): String =
        text.replace('\n', ' ').let { if (it.length <= 80) it else it.take(79) + "…" }

    private fun buildProperties(
        node: ComposeNode,
        absX: Int,
        absY: Int,
        text: String?,
    ): List<ViewProperty> {
        val rect = node.bounds
        val base = listOf(
            ViewProperty(
                "layout:getLocationOnScreen_x()", "getLocationOnScreen_x()", LAYOUT_CATEGORY,
                absX.toString(), false, absX,
            ),
            ViewProperty(
                "layout:getLocationOnScreen_y()", "getLocationOnScreen_y()", LAYOUT_CATEGORY,
                absY.toString(), false, absY,
            ),
            ViewProperty("compose:id", "id", CATEGORY, node.id.toString(), false, node.id.toInt()),
            ViewProperty("compose:file", "file", CATEGORY, node.file),
            ViewProperty("compose:line_number", "line_number", CATEGORY, node.line.toString(), false, node.line),
            ViewProperty("compose:offset", "offset", CATEGORY, node.offset.toString(), false, node.offset),
            ViewProperty(
                "compose:bounds", "bounds", CATEGORY,
                "[${rect.left},${rect.top}][${rect.right},${rect.bottom}]",
            ),
            ViewProperty(
                "compose:recompose_count", "recompose_count", CATEGORY,
                node.recomposeCount.toString(), false, node.recomposeCount,
            ),
            ViewProperty(
                "compose:recompose_skips", "recompose_skips", CATEGORY,
                node.recomposeSkips.toString(), false, node.recomposeSkips,
            ),
            ViewProperty("compose:flags", "flags", CATEGORY, formatFlags(node.flags)),
            ViewProperty("compose:view_id", "view_id", CATEGORY, node.viewId.toString()),
        )
        if (text == null) {
            return base
        }
        return listOf(ViewProperty("compose:text", "text", CATEGORY, text)) + base
    }

    private fun formatFlags(flags: Int): String {
        if (flags == 0) {
            return "0 (NONE)"
        }
        val names = FLAG_NAMES.filterKeys { flags and it != 0 }.values
        val base = if (names.isEmpty()) "0x${flags.toString(16)}" else names.joinToString("|")
        return "$flags ($base)"
    }
}
