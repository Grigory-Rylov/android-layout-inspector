package com.github.grishberg.android.layoutinspector.ui.tree

import java.awt.Color

class TextForegroundColor(
    private val default: Color,
    private val selection: Color,
    private val hidden: Color,
    private val selectedHidden: Color,
    private val hovered: Color,
    private val selectedHovered: Color,
    private val highlighted: Color,
    private val selectedHighlighted: Color
) {
    fun textForeground(isSelected: Boolean, isHovered: Boolean, isHighlighted: Boolean, isVisible: Boolean): Color {
        /*
        if (isHovered) {
            if (isSelected) {
                return selectedHovered
            }
            return hovered
        }
        */
        if (isHighlighted) {
            if (isSelected) {
                return selectedHighlighted
            }
            return highlighted
        }

        if (!isVisible) {
            if (isSelected) {
                return selectedHidden
            }
            return hidden
        }
        if (isSelected) {
            return selection
        }

        return default
    }
}