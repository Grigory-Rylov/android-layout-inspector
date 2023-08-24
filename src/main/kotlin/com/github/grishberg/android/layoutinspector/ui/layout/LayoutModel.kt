package com.github.grishberg.android.layoutinspector.ui.layout

import com.github.grishberg.android.layoutinspector.domain.AbstractViewNode
import java.awt.Shape

data class LayoutModel(
    val rect: Shape,
    val node: AbstractViewNode,
    val children: List<LayoutModel>)
