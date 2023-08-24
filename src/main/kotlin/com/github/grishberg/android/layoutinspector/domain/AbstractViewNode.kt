package com.github.grishberg.android.layoutinspector.domain

import javax.swing.tree.TreeNode

interface AbstractViewNode : TreeNode {

    val children: List<AbstractViewNode>
    val id: String?
    val name: String
    val locationOnScreenX: Int
    val locationOnScreenY: Int
    val width: Int
    val height: Int
    val isVisible: Boolean
    val hash: String
    val typeAsString: String
    val text: String?

    fun cloneWithNewParent(newParent: AbstractViewNode): AbstractViewNode
}
