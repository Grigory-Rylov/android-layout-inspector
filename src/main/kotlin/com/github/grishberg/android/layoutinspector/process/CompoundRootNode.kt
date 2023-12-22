package com.github.grishberg.android.layoutinspector.process

import com.github.grishberg.android.layoutinspector.domain.AbstractViewNode
import java.util.Collections
import java.util.Enumeration
import javax.swing.tree.TreeNode
import kotlin.math.max

class CompoundRootNode(
    firstRoot: AbstractViewNode,
    secondRoot: AbstractViewNode,
) : AbstractViewNode {

    override val children: List<AbstractViewNode> = listOf(firstRoot, secondRoot)
    override val id: String? = null
    override val name: String = "Root"
    override val locationOnScreenX: Int = 0
    override val locationOnScreenY: Int = 0
    override val width: Int = max(firstRoot.width, secondRoot.width)
    override val height: Int = max(firstRoot.height, secondRoot.height)
    override val isVisible: Boolean = true
    override val hash: String = "0000"
    override val typeAsString: String = "LayoutsAndHierarchyDumpRoot"
    override val text: String? = null

    override fun cloneWithNewParent(newParent: AbstractViewNode): AbstractViewNode {
        throw IllegalStateException("I was not ready for this operation =(")
    }

    override fun getChildAt(childIndex: Int): TreeNode = children[childIndex]

    override fun getChildCount(): Int = children.size

    override fun getParent(): TreeNode? = null

    override fun getIndex(node: TreeNode?): Int = children.indexOf(node)

    override fun getAllowsChildren(): Boolean = true

    override fun isLeaf(): Boolean = false

    override fun children(): Enumeration<out TreeNode> = Collections.enumeration(children)
}
