package com.github.grishberg.android.layoutinspector.domain

import java.util.Collections
import java.util.Enumeration
import javax.swing.tree.TreeNode

data class DumpViewNode(
    val parent: DumpViewNode?,
    val pkg: String,
    override val name: String,
    override val id: String?,
    val globalLeft: Int,
    val globalTop: Int,
    val globalRight: Int,
    val globalBottom: Int,
    override val text: String?,
) : AbstractViewNode {

    override val locationOnScreenX: Int = globalLeft
    override val locationOnScreenY: Int = globalTop

    override val children = mutableListOf<AbstractViewNode>()
    override val width: Int = globalRight - globalLeft
    override val height: Int = globalBottom - globalTop
    override val isVisible: Boolean = true
    override val hash: String = "<dump>"

    override val typeAsString: String

    init {
        val lastDotPost = name.lastIndexOf(".")
        typeAsString = if (lastDotPost >= 0) {
            name.substring(lastDotPost + 1)
        } else {
            name
        }
    }

    fun addChild(child: DumpViewNode) {
        children.add(child)
    }

    override fun getChildAt(childIndex: Int): TreeNode = children[childIndex]

    override fun getChildCount(): Int = children.size

    override fun getParent(): TreeNode? = parent

    override fun getIndex(node: TreeNode?): Int = children.indexOf(node)

    override fun getAllowsChildren(): Boolean = true

    override fun isLeaf(): Boolean = childCount == 0

    override fun children(): Enumeration<out TreeNode> = Collections.enumeration(children)
}
