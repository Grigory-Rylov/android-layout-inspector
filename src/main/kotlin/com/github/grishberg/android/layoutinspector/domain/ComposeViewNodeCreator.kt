package com.github.grishberg.android.layoutinspector.domain

import com.github.grishberg.android.layoutinspector.settings.TreeSettings
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.ComposableNode
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.GetComposablesResponse
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.Quad
import java.util.EnumSet

/**
 * Helper class for creating [ComposeViewNode]s.
 *
 * @param result A compose tree received from the compose agent
 */
class ComposeViewNodeCreator(
    private val result: GetComposablesResult,
    private val treeSettings: TreeSettings
) {
    private val stringTable = result.response.stringsList
    private val roots = result.response.rootsList.associateBy { it.viewId }
    private val androidViews = mutableMapOf<Long, MutableList<Long>>()
    private var nodesCreated = false
    private var composeFlags = 0

    /** The collected capabilities based on the loaded data. */
    val dynamicCapabilities: Set<Capability>
        get() {
            val capabilities = mutableSetOf<Capability>()
            if (nodesCreated) {
                capabilities.add(Capability.SUPPORTS_COMPOSE)
                if (composeFlags and ComposableNode.Flags.HAS_MERGED_SEMANTICS_VALUE != 0) {
                    capabilities.add(Capability.SUPPORTS_SEMANTICS)
                }
                if (composeFlags and ComposableNode.Flags.SYSTEM_CREATED_VALUE != 0) {
                    capabilities.add(Capability.SUPPORTS_SYSTEM_NODES)
                }
            }
            return capabilities
        }

    /** A map of view IDs to a list of view IDs that should be skipped. */
    val viewsToSkip: Map<Long, List<Long>>
        get() = androidViews

    fun createForViewId(id: Long, shouldInterrupt: () -> Boolean): List<ComposeViewNode>? {
        androidViews.clear()
        val result = ViewNode.writeAccess { roots[id]?.map { node -> node.convert(shouldInterrupt, this) } }
        nodesCreated = nodesCreated || (result?.isNotEmpty() ?: false)
        return result
    }

    private fun ComposableNode.convert(
        shouldInterrupt: () -> Boolean,
        access: ViewNode.WriteAccess,
    ): ComposeViewNode {
        if (shouldInterrupt()) {
            throw InterruptedException()
        }

        val layoutBounds = bounds.layout.toRectangle()

        // The Quad coordinates are supplied relative to the View that contains the composables.
        // We need to convert them to the coordinates the inspector works in.
        val renderBounds = bounds.render.takeIf { it != Quad.getDefaultInstance() }?.toPolygon() ?: layoutBounds
        val actualFlags = if (packageHash != -1) flags else flags and ComposableNode.Flags.SYSTEM_CREATED_VALUE.inv()
        val isSystemNode = (flags and ComposableNode.Flags.SYSTEM_CREATED_VALUE) != 0
        val ignoreRecompositions = result.pendingRecompositionCountReset ||
                (isSystemNode && treeSettings.ignoreRecompositionsInFramework)

        val node = ComposeViewNode(
            id,
            stringTable[name],
            null,
            layoutBounds,
            renderBounds,
            null,
            "",
            0,
            if (ignoreRecompositions) 0 else recomposeCount,
            if (ignoreRecompositions) 0 else recomposeSkips,
            stringTable[filename],
            packageHash,
            offset,
            lineNumber,
            actualFlags,
            anchorHash,
        )

        composeFlags = composeFlags or actualFlags

        access.apply {
            childrenList.forEach { child ->
                val childNode = child.convert(shouldInterrupt, this)
                node.children.add(childNode)
                childNode.parent = node
            }
        }

        return node
    }
}

class GetComposablesResult(
    /** The response received from the agent */
    val response: GetComposablesResponse,

    /**
     * This is true, if a recomposition count reset command was sent after the GetComposables command
     * was sent.
     */
    val pendingRecompositionCountReset: Boolean,
) 