package com.github.grishberg.android.layoutinspector.ui.theme

import com.github.grishberg.android.layoutinspector.ui.tree.IconsStore
import java.awt.Color
import javax.swing.UIManager

class MaterialDarkColors : ThemeColors {
    private val primaryColor = Color(0xBB86CC)
    private val secondaryColor = Color(0x000000)
    private val errorColor = Color(0xCF6679)
    private val foregroundColor = Color(0xffffff)
    private val foregroundMediumColor = Color(255, 255, 255, 153)

    override val treeBackground = UIManager.getColor("Tree.background")
    override val selectionBackground = Color(249, 192, 98, 0)
    override val hoverBackground = Color(foregroundColor.red, foregroundColor.green, foregroundColor.blue, 30)


    override val foreground1 = foregroundColor
    override val selectionForeground1 = foregroundColor
    override val hiddenForeground1 = Color(157, 157, 157)
    override val hoveredForeground1 = UIManager.getColor("Button.mouseHoverColor") ?: foregroundColor
    override val selectionHiddenForeground1 = Color(selectionForeground1.red, selectionForeground1.green, selectionForeground1.blue, 220)

    override val foreground2 = foregroundMediumColor
    override val selectionForeground2 = Color(selectionForeground1.red, selectionForeground1.green, selectionForeground1.blue, 220)
    override val hiddenForeground2 =  Color(130, 130, 130)
    override val hoveredForeground2 = UIManager.getColor("Button.mouseHoverColor") ?: foreground2
    override val selectionHiddenForeground2 = secondaryColor

    override val foundTextColor = errorColor
    override val selectedFoundTextColor = primaryColor

    override val groupForeground = Color(187, 187, 187)
    override val groupBackground = Color(86, 86, 86)
    override val propertiesPanelHovered = Color(70, 70, 70)
    override val propertiesPanelBackground= Color(44, 44, 44)

    private val iconsStore = IconsStore()
    override val textIcon = iconsStore.createImageIcon("/icons/dark/text.png")
    override val fabIcon = iconsStore.createImageIcon("/icons/dark/fab.png")
    override val appBarIcon = iconsStore.createImageIcon("/icons/dark/appbar.png")
    override val coordinatorLayoutIcon = iconsStore.createImageIcon("/icons/dark/coordinator_layout.png")
    override val constraintLayoutIcon = iconsStore.createImageIcon("/icons/dark/constraint_layout.png")
    override val frameLayoutIcon = iconsStore.createImageIcon("/icons/dark/frame_layout.png")
    override val linearLayoutIcon = iconsStore.createImageIcon("/icons/dark/linear_layout.png")
    override val cardViewIcon = iconsStore.createImageIcon("/icons/dark/cardView.png")
    override val viewStubIcon = iconsStore.createImageIcon("/icons/dark/viewstub.png")
    override val toolbarIcon = iconsStore.createImageIcon("/icons/dark/toolbar.png")
    override val listViewIcon = iconsStore.createImageIcon("/icons/dark/recyclerView.png")
    override val relativeLayoutIcon = iconsStore.createImageIcon("/icons/dark/relativeLayout.png")
    override val imageViewIcon = iconsStore.createImageIcon("/icons/dark/imageView.png")
    override val nestedScrollViewIcon = iconsStore.createImageIcon("/icons/dark/nestedScrollView.png")
    override val viewSwitcherIcon = iconsStore.createImageIcon("/icons/dark/viewSwitcher.png")
    override val viewPagerIcon = iconsStore.createImageIcon("/icons/dark/viewPager.png")
    override val viewIcon = iconsStore.createImageIcon("/icons/dark/view.png")
}
