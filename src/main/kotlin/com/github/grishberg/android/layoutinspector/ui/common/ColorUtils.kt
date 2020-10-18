package com.github.grishberg.android.layoutinspector.ui.common

import java.awt.Color

fun hexToColor(colorInHex: String?): Color? {
    if (colorInHex == null) return null
    val colorInt = Integer.parseInt(colorInHex, 16)
    return Color(colorInt)
}

fun colorToHex(color: Color?): String? {
    if (color == null) {
        return null
    }
    return "%x%x%x".format(color.red, color.green, color.blue)
}
