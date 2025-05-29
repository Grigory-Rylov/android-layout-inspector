package com.github.grishberg.android.layoutinspector.domain

import java.awt.Rectangle
import java.awt.Shape

/** A view node represents a composable in the view hierarchy as seen on the device. */
class ComposeViewNode(
    drawId: Long,
    qualifiedName: String,
    layout: ResourceReference?,
    layoutBounds: Rectangle,
    renderBounds: Shape,
    viewId: ResourceReference?,
    textValue: String,
    layoutFlags: Int,

    /**
     * The number of times this node was recomposed (i.e. the composable method called) since last
     * reset.
     */
    recomposeCount: Int,

    /**
     * The number of times this node was skipped (i.e. the composable method was not called when it
     * might have been) since last reset.
     */
    recomposeSkips: Int,

    /** The filename of where the code for this composable resides. This name not contain a path. */
    var composeFilename: String,

    /** The hash of the package name where the composable resides. */
    var composePackageHash: Int,

    /** The offset to the method start in the file where the composable resides. */
    var composeOffset: Int,

    /** The line number of the method start in the file where the composable resides. */
    var composeLineNumber: Int,

    /** Flags as defined by the FLAG_* constants above. */
    var composeFlags: Int,

    /** The hash of an anchor which can identify the composable after a recomposition. */
    var anchorHash: Int,
) : ViewNode(
    drawId,
    qualifiedName,
    layout,
    layoutBounds,
    renderBounds,
    viewId,
    textValue,
    layoutFlags
) {
    val recompositions = RecompositionInfo(recomposeCount, recomposeSkips)

    /** Information about recompositions of this node. */
    class RecompositionInfo(
        var count: Int,
        var skips: Int
    ) {
        /** The highlight count is a value between 0 and 1 indicating how recently this node was recomposed. */
        var highlightCount = 0f

        fun update(other: RecompositionInfo) {
            count = other.count
            skips = other.skips
            highlightCount = other.highlightCount
        }
    }
} 