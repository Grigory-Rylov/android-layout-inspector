package com.github.grishberg.android.layoutinspector.ui.theme

import java.awt.Color
import javax.swing.ImageIcon

interface ThemeColors {
    val treeBackground: Color
    val selectionBackground: Color
    val hoverBackground: Color

    val foreground1: Color
    val selectionForeground1: Color

    val foreground2: Color
    val selectionForeground2: Color

    val hiddenForeground1: Color
    val hiddenForeground2: Color

    val hoveredForeground1: Color
    val hoveredForeground2: Color

    val groupForeground: Color
    val groupBackground: Color

    val propertiesPanelHovered: Color
    val propertiesPanelBackground: Color

    val selectionHiddenForeground1: Color
    val selectionHiddenForeground2: Color
    val foundTextColor: Color
    val selectedFoundTextColor: Color

    val textIcon: ImageIcon
    val fabIcon: ImageIcon
    val appBarIcon: ImageIcon
    val coordinatorLayoutIcon: ImageIcon
    val constraintLayoutIcon: ImageIcon
    val frameLayoutIcon: ImageIcon
    val linearLayoutIcon: ImageIcon
    val cardViewIcon: ImageIcon
    val viewStubIcon: ImageIcon
    val toolbarIcon: ImageIcon
    val listViewIcon: ImageIcon
    val relativeLayoutIcon: ImageIcon
    val imageViewIcon: ImageIcon
    val nestedScrollViewIcon: ImageIcon
    val viewSwitcherIcon: ImageIcon
    val viewPagerIcon: ImageIcon
    val viewIcon: ImageIcon

    fun addColorChangedAction(action: () -> Unit) = Unit
    fun removeColorChangedAction(action: () -> Unit) = Unit

}
