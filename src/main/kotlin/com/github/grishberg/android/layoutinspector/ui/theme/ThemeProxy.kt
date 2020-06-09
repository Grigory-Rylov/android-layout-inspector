package com.github.grishberg.android.layoutinspector.ui.theme

import java.awt.Color

class ThemeProxy : ThemeColors {
    var currentTheme: ThemeColors = MaterialLiteTheme()

    override val selectionHoveredText1Color: Color
        get() = currentTheme.selectionHoveredText1Color

    override val selectionHoveredText2Color: Color
        get() = currentTheme.selectionHoveredText2Color

    override val selectionHiddenText1Color: Color
        get() = currentTheme.selectionHiddenText1Color

    override val selectionHiddenText2Color: Color
        get() = currentTheme.selectionHiddenText2Color

    override val foundTextColor: Color
        get() = currentTheme.foundTextColor

    override val selectedFoundTextColor: Color
        get() = currentTheme.selectedFoundTextColor

    override val text2ForegroundColor: Color
        get() = currentTheme.text2ForegroundColor

    override val selectionForeground2: Color
        get() = currentTheme.selectionForeground2

    override val selectionBackground: Color
        get() = currentTheme.selectionBackground

    override val text1ForegroundColor: Color
        get() = currentTheme.text1ForegroundColor

    override val selectionForeground1: Color
        get() = currentTheme.selectionForeground1

    override val hiddenText1Color: Color
        get() = currentTheme.hiddenText1Color

    override val hiddenText2Color: Color
        get() = currentTheme.hiddenText2Color

    override val hoveredText1Color: Color
        get() = currentTheme.hoveredText1Color

    override val hoveredText2Color: Color
        get() = currentTheme.hoveredText2Color
}
