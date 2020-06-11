package com.github.grishberg.android.layoutinspector.ui.theme

import com.github.grishberg.android.layoutinspector.ui.tree.IconsStore
import java.awt.Color
import javax.swing.UIManager

class MaterialOceanColors : ThemeColors {
    override val treeBackground = UIManager.getColor("Tree.background")

    override val hoverBackground: Color
        get() = Color(treeBackground.red, treeBackground.green, treeBackground.blue, 200)


    override val selectionBackground = UIManager.getColor("Tree.selectionBackground")

    override val foreground1 = UIManager.getColor("Tree.textForeground")
    override val selectionForeground1 = UIManager.getColor("Tree.selectionForeground")

    override val foreground2 = Color(0, 0, 0, 127)

    override val selectionForeground2 = UIManager.getColor("Tree.selectionForeground")

    override val hiddenForeground1 = UIManager.getColor("Label.disabledForeground")
    override val hiddenForeground2 = UIManager.getColor("Label.disabledForeground")

    override val hoveredForeground1 = UIManager.getColor("Button.mouseHoverColor")

    override val hoveredForeground2 = UIManager.getColor("Button.mouseHoverColor")

    override val selectionHiddenForeground1 = Color(150, 180, 250)

    override val selectionHiddenForeground2 = Color(150, 180, 250)

    override val foundTextColor = Color(204, 42, 49)

    override val selectedFoundTextColor = Color(255, 202, 185)

    private val iconsStore = IconsStore()
    override val textIcon = iconsStore.createImageIcon("icons/dark/text.png")
    override val fabIcon = iconsStore.createImageIcon("icons/dark/fab.png")
    override val appBarIcon = iconsStore.createImageIcon("icons/dark/appbar.png")
    override val coordinatorLayoutIcon = iconsStore.createImageIcon("icons/dark/coordinator_layout.png")
    override val constraintLayoutIcon = iconsStore.createImageIcon("icons/dark/constraint_layout.png")
    override val frameLayoutIcon = iconsStore.createImageIcon("icons/dark/frame_layout.png")
    override val linearLayoutIcon = iconsStore.createImageIcon("icons/dark/linear_layout.png")
    override val cardViewIcon = iconsStore.createImageIcon("icons/dark/cardView.png")
    override val viewStubIcon = iconsStore.createImageIcon("icons/dark/viewstub.png")
    override val toolbarIcon = iconsStore.createImageIcon("icons/dark/toolbar.png")
    override val listViewIcon = iconsStore.createImageIcon("icons/dark/recyclerView.png")
    override val relativeLayoutIcon = iconsStore.createImageIcon("icons/dark/relativeLayout.png")
    override val imageViewIcon = iconsStore.createImageIcon("icons/dark/imageView.png")
    override val nestedScrollViewIcon = iconsStore.createImageIcon("icons/dark/nestedScrollView.png")
    override val viewSwitcherIcon = iconsStore.createImageIcon("icons/dark/viewSwitcher.png")
    override val viewPagerIcon = iconsStore.createImageIcon("icons/dark/viewPager.png")
    override val viewIcon = iconsStore.createImageIcon("icons/dark/view.png")
}