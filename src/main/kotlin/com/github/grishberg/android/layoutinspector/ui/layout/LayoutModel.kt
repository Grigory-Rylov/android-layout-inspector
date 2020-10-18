package com.github.grishberg.android.layoutinspector.ui.layout

import com.android.layoutinspector.model.ViewNode
import java.awt.Shape

data class LayoutModel(
    val rect: Shape,
    val node: ViewNode,
    val children: List<LayoutModel>)
