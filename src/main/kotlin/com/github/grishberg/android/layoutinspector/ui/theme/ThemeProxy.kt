package com.github.grishberg.android.layoutinspector.ui.theme

import java.awt.Color
import javax.swing.ImageIcon

class ThemeProxy : ThemeColors {
    var currentTheme: ThemeColors = MaterialLiteColors()
        set(value) {
            field = value
            actions.forEach {
                it.invoke()
            }
        }

    override val hoverBackground: Color
        get() = currentTheme.hoverBackground

    override val treeBackground: Color
        get() = currentTheme.treeBackground

    override val selectionHiddenForeground1: Color
        get() = currentTheme.selectionHiddenForeground1

    override val selectionHiddenForeground2: Color
        get() = currentTheme.selectionHiddenForeground2

    override val foundTextColor: Color
        get() = currentTheme.foundTextColor

    override val selectedFoundTextColor: Color
        get() = currentTheme.selectedFoundTextColor

    override val foreground2: Color
        get() = currentTheme.foreground2

    override val selectionForeground2: Color
        get() = currentTheme.selectionForeground2

    override val selectionBackground: Color
        get() = currentTheme.selectionBackground

    override val foreground1: Color
        get() = currentTheme.foreground1

    override val selectionForeground1: Color
        get() = currentTheme.selectionForeground1

    override val hiddenForeground1: Color
        get() = currentTheme.hiddenForeground1

    override val hiddenForeground2: Color
        get() = currentTheme.hiddenForeground2

    override val hoveredForeground1: Color
        get() = currentTheme.hoveredForeground1

    override val hoveredForeground2: Color
        get() = currentTheme.hoveredForeground2


    override val textIcon: ImageIcon
        get() = currentTheme.textIcon

    override val fabIcon: ImageIcon
        get() = currentTheme.fabIcon

    override val appBarIcon: ImageIcon
        get() = currentTheme.appBarIcon

    override val coordinatorLayoutIcon: ImageIcon
        get() = currentTheme.coordinatorLayoutIcon

    override val constraintLayoutIcon: ImageIcon
        get() = currentTheme.constraintLayoutIcon

    override val frameLayoutIcon: ImageIcon
        get() = currentTheme.frameLayoutIcon

    override val linearLayoutIcon: ImageIcon
        get() = currentTheme.linearLayoutIcon

    override val cardViewIcon: ImageIcon
        get() = currentTheme.cardViewIcon

    override val viewStubIcon: ImageIcon
        get() = currentTheme.viewStubIcon

    override val toolbarIcon: ImageIcon
        get() = currentTheme.toolbarIcon

    override val listViewIcon: ImageIcon
        get() = currentTheme.listViewIcon

    override val relativeLayoutIcon: ImageIcon
        get() = currentTheme.relativeLayoutIcon

    override val imageViewIcon: ImageIcon
        get() = currentTheme.imageViewIcon

    override val nestedScrollViewIcon: ImageIcon
        get() = currentTheme.nestedScrollViewIcon

    override val viewSwitcherIcon: ImageIcon
        get() = currentTheme.viewSwitcherIcon

    override val viewPagerIcon: ImageIcon
        get() = currentTheme.viewPagerIcon

    override val viewIcon: ImageIcon
        get() = currentTheme.nestedScrollViewIcon

    override val groupForeground: Color
        get() = currentTheme.groupForeground

    override val groupBackground: Color
        get() = currentTheme.groupBackground

    override val propertiesPanelHovered: Color
        get() = currentTheme.propertiesPanelHovered

    override val propertiesPanelBackground: Color
        get() = currentTheme.propertiesPanelBackground

    private val actions = mutableListOf<() -> Unit>()

    override fun addColorChangedAction(action: () -> Unit) {
        actions.add(action)
    }

    override fun removeColorChangedAction(action: () -> Unit) {
        actions.remove(action)
    }
}
