package com.github.grishberg.android.layoutinspector.ui.tree

import com.android.layoutinspector.model.ViewNode
import java.awt.Color
import java.awt.Component
import javax.swing.ImageIcon
import javax.swing.JTree
import javax.swing.UIManager
import javax.swing.tree.TreeCellRenderer


class NodeViewTreeCellRenderer(
    private val foundItems: List<ViewNode>
) : TreeCellRenderer {
    var hoveredNode: ViewNode? = null
    private val iconsStore = IconsStore()

    private val textIcon = iconsStore.createImageIcon("icons/text.png")
    private val fabIcon = iconsStore.createImageIcon("icons/fab.png")
    private val appBarIcon = iconsStore.createImageIcon("icons/appbar.png")
    private val coordinatorLayoutIcon = iconsStore.createImageIcon("icons/coordinator_layout.png")
    private val constraintLayoutIcon = iconsStore.createImageIcon("icons/constraint_layout.png")
    private val frameLayoutIcon = iconsStore.createImageIcon("icons/frame_layout.png")
    private val linearLayoutIcon = iconsStore.createImageIcon("icons/linear_layout.png")
    private val cardViewIcon = iconsStore.createImageIcon("icons/cardView.png")
    private val viewStubIcon = iconsStore.createImageIcon("icons/viewstub.png")
    private val toolbarIcon = iconsStore.createImageIcon("icons/toolbar.png")
    private val listViewIcon = iconsStore.createImageIcon("icons/recyclerView.png")
    private val relativeLsyoutIcon = iconsStore.createImageIcon("icons/relativeLayout.png")
    private val imageViewIcon = iconsStore.createImageIcon("icons/imageView.png")
    private val nestedScrollViewIcon = iconsStore.createImageIcon("icons/nestedScrollView.png")
    private val viewSwitcherIcon = iconsStore.createImageIcon("icons/viewSwitcher.png")
    private val viewPagerIcon = iconsStore.createImageIcon("icons/viewPager.png")
    private val viewIcon = iconsStore.createImageIcon("icons/view.png")

    private val text1ForegroundColor = UIManager.getColor("Tree.textForeground")
    private val textBackground: Color = UIManager.getColor("Tree.textBackground")

    private val selectionForeground1 = UIManager.getColor("Tree.selectionForeground")

    private val text2ForegroundColor = Color(0, 0, 0, 127)
    private val selectionForeground2: Color = Color(0, 0, 0, 127)

    private val hoveredText1Color = Color(45, 71, 180)
    private val hoveredText2Color = Color(57, 90, 227)

    private val selectionHoveredText1Color = Color(240, 245, 248)
    private val selectionHoveredText2Color = Color(186, 225, 255, 220)

    private val hiddenText1Color = Color(0, 0, 0, 127)
    private val hiddenText2Color = Color(0, 0, 0, 90)

    private val selectionHiddenText1Color = Color(220, 220, 220)
    private val selectionHiddenText2Color = Color(220, 220, 220)

    private val foundTextColor = Color(204, 42, 49)
    private val selectedFoundTextColor = Color(255, 202, 185)

    private val text1Foreground =
        TextForegroundColor(
            text1ForegroundColor,
            selectionForeground1,
            hiddenText1Color,
            selectionHiddenText1Color,
            hoveredText1Color,
            selectionHoveredText1Color,
            foundTextColor,
            selectedFoundTextColor
        )
    private val text2Foreground =
        TextForegroundColor(
            text2ForegroundColor,
            selectionForeground2,
            hiddenText2Color,
            selectionHiddenText2Color,
            hoveredText2Color,
            selectionHoveredText2Color,
            foundTextColor,
            selectedFoundTextColor
        )

    private val textViewRenderer = TextViewRenderer(textIcon)
    private val defaultCellRenderer = SimpleViewNodeRenderer()

    override fun getTreeCellRendererComponent(
        tree: JTree?,
        value: Any?,
        selected: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean
    ): Component {
        defaultCellRenderer.getTreeCellRendererComponent(
            tree,
            value,
            selected,
            expanded,
            leaf,
            row,
            hasFocus
        )
        if (value !is ViewNode) {
            return defaultCellRenderer
        }
        val hovered = value == hoveredNode
        val visible = value.isDrawn
        val text = value.getText()
        val highlighted = foundItems.contains(value)

        val isTextView = text != null
        val item: TreeItem = if (isTextView) {
            textViewRenderer
        } else {
            defaultCellRenderer
        }

        var foreground1 = text1Foreground.textForeground(selected, hovered, highlighted, visible)
        var foreground2 = text2Foreground.textForeground(selected, hovered, highlighted, visible)
        item.setForeground(foreground1, foreground2)

        if (text != null) {
            item.setTitle(value.typeAsString(), value.getElliptizedText(text))
            textViewRenderer.selected = selected
            textViewRenderer.hovered = hovered
            return textViewRenderer
        }
        item.setTitle(value.getFormattedName())
        item.setIcon(iconForNode(value))
        return defaultCellRenderer
    }

    private fun iconForNode(node: ViewNode): ImageIcon {
        val nodeTypeShort = node.typeAsString()

        if (node.name == "android.view.ViewStub") {
            return viewStubIcon
        }

        if (node.name == "android.widget.FrameLayout" || node.name == "androidx.appcompat.widget.ContentFrameLayout" ||
            node.name == "androidx.appcompat.widget.FitWindowsFrameLayout"
        ) {
            return frameLayoutIcon
        }

        if (nodeTypeShort == "AppBarLayout" || node.name == "com.android.internal.widget.ActionBarContainer") {
            return appBarIcon
        }

        if (nodeTypeShort == "ConstraintLayout") {
            return constraintLayoutIcon
        }

        if (nodeTypeShort == "CollapsingToolbarLayout" || nodeTypeShort == "Toolbar") {
            return toolbarIcon
        }

        if (nodeTypeShort == "CoordinatorLayout") {
            return coordinatorLayoutIcon
        }

        if (nodeTypeShort == "AppCompatImageButton" || nodeTypeShort == "ImageButton" || nodeTypeShort == "ImageView") {
            return imageViewIcon
        }

        if (nodeTypeShort == "ViewSwitcher") {
            return viewSwitcherIcon
        }

        if (nodeTypeShort == "NestedScrollView") {
            return nestedScrollViewIcon
        }

        if (nodeTypeShort.contains("RecyclerView")) {
            return listViewIcon
        }

        if (nodeTypeShort.contains("RelativeLayout")) {
            return relativeLsyoutIcon
        }

        if (nodeTypeShort.endsWith("CardView")) {
            return cardViewIcon
        }
        if (nodeTypeShort.endsWith("ViewPager")) {
            return viewPagerIcon
        }
        if (nodeTypeShort == "FloatingActionButton") {
            return fabIcon
        }
        if (nodeTypeShort.endsWith("LinearLayout")) {
            return linearLayoutIcon
        }
        return viewIcon
    }

}