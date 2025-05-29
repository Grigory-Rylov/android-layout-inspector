package com.github.grishberg.android.layoutinspector.domain

import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol
import java.awt.Polygon
import java.awt.Rectangle

fun LayoutInspectorComposeProtocol.Quad.toPolygon(): Polygon {
    return Polygon(intArrayOf(x0, x1, x2, x3), intArrayOf(y0, y1, y2, y3), 4)
}

fun LayoutInspectorComposeProtocol.Rect.toRectangle() = Rectangle(x, y, w, h) 