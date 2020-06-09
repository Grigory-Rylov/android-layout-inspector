package com.github.grishberg.android.layoutinspector.ui.theme

import java.awt.Color
import javax.swing.UIManager

class MaterialLiteTheme : ThemeColors {
    override val selectionBackground: Color
        get() = UIManager.getColor("Tree.selectionBackground")

    override val text1ForegroundColor: Color
        get() = UIManager.getColor("Tree.textForeground")
    override val selectionForeground1: Color
        get() = UIManager.getColor("Tree.selectionForeground")

    override val text2ForegroundColor: Color
        get() = Color(0, 0, 0, 127)

    override val selectionForeground2: Color
        get() = UIManager.getColor("Tree.selectionForeground")

    override val hiddenText1Color: Color
        get() = UIManager.getColor("Label.disabledForeground")
    override val hiddenText2Color: Color
        get() = UIManager.getColor("Label.disabledForeground")

    override val hoveredText1Color: Color
        get() = UIManager.getColor("Button.mouseHoverColor") //Color(45, 71, 180)

    override val hoveredText2Color: Color
        get() = UIManager.getColor("Button.mouseHoverColor") //Color(57, 90, 227)

    override val selectionHoveredText1Color: Color
        get() = Color(75, 153, 162)

    override val selectionHoveredText2Color: Color
        get() = Color(75, 153, 162, 220)

    override val selectionHiddenText1Color: Color
        get() = Color(150, 180, 250)

    override val selectionHiddenText2Color: Color
        get() = Color(150, 180, 250)

    override val foundTextColor: Color
        get() = Color(204, 42, 49)

    override val selectedFoundTextColor: Color
        get() = Color(255, 202, 185)
}
