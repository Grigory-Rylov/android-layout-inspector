package com.github.grishberg.android.li.compose.model

import java.util.Collections
import java.util.Enumeration
import javax.swing.tree.TreeNode

class ComposeCandidateNode(
    val className: String,
    val resourceId: String?,
    val text: String?,
    val contentDescription: String?,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val isComposeCandidate: Boolean,
    private val parentNode: ComposeCandidateNode?,
) : TreeNode {
    val children = mutableListOf<ComposeCandidateNode>()
    val width: Int get() = right - left
    val height: Int get() = bottom - top

    fun add(child: ComposeCandidateNode) {
        children.add(child)
    }

    fun displayName(): String {
        val short = className.substringAfterLast('.')
        val parts = mutableListOf(short)
        if (!resourceId.isNullOrEmpty()) parts += "#$resourceId"
        if (!text.isNullOrEmpty()) parts += "text=\"${text.take(40)}\""
        if (!contentDescription.isNullOrEmpty()) parts += "desc=\"${contentDescription.take(40)}\""
        if (isComposeCandidate) parts += "[compose]"
        parts += "(${width}x${height})"
        return parts.joinToString("  ")
    }

    override fun getChildAt(childIndex: Int): TreeNode = children[childIndex]
    override fun getChildCount(): Int = children.size
    override fun getParent(): TreeNode? = parentNode
    override fun getIndex(node: TreeNode?): Int = children.indexOf(node)
    override fun getAllowsChildren(): Boolean = true
    override fun isLeaf(): Boolean = children.isEmpty()
    override fun children(): Enumeration<out TreeNode> = Collections.enumeration(children)
}
