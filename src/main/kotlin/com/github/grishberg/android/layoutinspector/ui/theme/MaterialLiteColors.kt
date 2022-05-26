package com.github.grishberg.android.layoutinspector.ui.theme

import com.github.grishberg.android.layoutinspector.ui.tree.IconsStore
import java.awt.Color
import javax.swing.UIManager

class MaterialLiteColors : ThemeColors {
    private val primaryColor = Color(0x6200EE)

    private val foregroundColor = Color(0x000000)
    private val foregroundMediumColor = Color(0, 0, 0, 153)

    override val treeBackground = UIManager.getColor("Tree.background")
    override val selectionBackground = Color(195, 219, 247,0)

    override val hoverBackground = Color(
        foregroundColor.red,
        foregroundColor.green,
        foregroundColor.blue,
        20
    )

    override val foreground1 = foregroundColor
    override val selectionForeground1 = Color(255, 255, 255)
    override val hiddenForeground1 = UIManager.getColor("Label.disabledForeground")
    override val hoveredForeground1 = foregroundColor
    override val selectionHiddenForeground1 = selectionForeground1

    override val foreground2 = foregroundMediumColor
    override val selectionForeground2 = foregroundMediumColor
    override val hiddenForeground2 = UIManager.getColor("Label.disabledForeground")
    override val hoveredForeground2 = selectionForeground2
    override val selectionHiddenForeground2 = hiddenForeground2

    override val foundTextColor = Color(204, 42, 49)
    override val selectedFoundTextColor = primaryColor

    override val groupForeground = Color.BLACK
    override val groupBackground = Color(242, 242, 242)
    override val propertiesPanelHovered = treeBackground
    override val propertiesPanelBackground = treeBackground

    private val iconsStore = IconsStore()

    override val textIcon = iconsStore.createImageIcon("/icons/light/text.png")
    override val fabIcon = iconsStore.createImageIcon("/icons/light/fab.png")
    override val appBarIcon = iconsStore.createImageIcon("/icons/light/appbar.png")
    override val coordinatorLayoutIcon = iconsStore.createImageIcon("/icons/light/coordinator_layout.png")
    override val constraintLayoutIcon = iconsStore.createImageIcon("/icons/light/constraint_layout.png")
    override val frameLayoutIcon = iconsStore.createImageIcon("/icons/light/frame_layout.png")
    override val linearLayoutIcon = iconsStore.createImageIcon("/icons/light/linear_layout.png")
    override val cardViewIcon = iconsStore.createImageIcon("/icons/light/cardView.png")
    override val viewStubIcon = iconsStore.createImageIcon("/icons/light/viewstub.png")
    override val toolbarIcon = iconsStore.createImageIcon("/icons/light/toolbar.png")
    override val listViewIcon = iconsStore.createImageIcon("/icons/light/recyclerView.png")
    override val relativeLayoutIcon = iconsStore.createImageIcon("/icons/light/relativeLayout.png")
    override val imageViewIcon = iconsStore.createImageIcon("/icons/light/imageView.png")
    override val nestedScrollViewIcon = iconsStore.createImageIcon("/icons/light/nestedScrollView.png")
    override val viewSwitcherIcon = iconsStore.createImageIcon("/icons/light/viewSwitcher.png")
    override val viewPagerIcon = iconsStore.createImageIcon("/icons/light/viewPager.png")
    override val viewIcon = iconsStore.createImageIcon("/icons/light/view.png")

}
