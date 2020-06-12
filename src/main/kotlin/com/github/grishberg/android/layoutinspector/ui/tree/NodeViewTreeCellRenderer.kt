package com.github.grishberg.android.layoutinspector.ui.tree

import com.android.layoutinspector.model.ViewNode
import com.github.grishberg.android.layoutinspector.ui.theme.ThemeColors
import java.awt.Component
import javax.swing.ImageIcon
import javax.swing.JTree
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.TreeCellRenderer


class NodeViewTreeCellRenderer(
    private val foundItems: List<ViewNode>,
    private val theme: ThemeColors
) : TreeCellRenderer {
    var hoveredNode: ViewNode? = null

    private val text1Foreground =
        TextForegroundColor(
            theme.foreground1,
            theme.selectionForeground1,
            theme.hiddenForeground1,
            theme.selectionHiddenForeground1,
            theme.hoveredForeground1,
            theme.selectionForeground1,
            theme.foundTextColor,
            theme.selectedFoundTextColor
        )
    private val text2Foreground =
        TextForegroundColor(
            theme.foreground2,
            theme.selectionForeground2,
            theme.hiddenForeground2,
            theme.selectionHiddenForeground2,
            theme.hoveredForeground2,
            theme.selectionForeground2,
            theme.foundTextColor,
            theme.selectedFoundTextColor
        )

    private val itemRenderer = ItemViewRenderer(theme)

    override fun getTreeCellRendererComponent(
        tree: JTree,
        value: Any,
        selected: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean
    ): Component {
        if (value !is ViewNode) {
            return DefaultTreeCellRenderer()
        }
        val hovered = value == hoveredNode
        val visible = value.isDrawn
        val text = value.getText()
        val highlighted = foundItems.contains(value)

        if (selected) {
            itemRenderer.setBackgroundSelectionColor(theme.selectionBackground)
        } else if (hovered) {
            itemRenderer.setBackgroundSelectionColor(theme.hoverBackground)
        }

        val foreground1 = text1Foreground.textForeground(selected, hovered, highlighted, visible)
        val foreground2 = text2Foreground.textForeground(selected, hovered, highlighted, visible)
        itemRenderer.setForeground(foreground1, foreground2)


        if (text != null) {
            itemRenderer.setIcon(theme.textIcon)
            itemRenderer.prepareTreeItem(
                value.typeAsString(),
                value.getElliptizedText(text),
                selected,
                expanded,
                leaf,
                hovered
            )
        } else {
            itemRenderer.setIcon(iconForNode(value))
            itemRenderer.prepareTreeItem(value.getFormattedName(), "", selected, expanded, leaf, hovered)
        }

        return itemRenderer
    }

    private fun iconForNode(node: ViewNode): ImageIcon {
        val nodeTypeShort = node.typeAsString()

        if (node.name == "android.view.ViewStub") {
            return theme.viewStubIcon
        }

        if (node.name == "android.widget.FrameLayout" || node.name == "androidx.appcompat.widget.ContentFrameLayout" ||
            node.name == "androidx.appcompat.widget.FitWindowsFrameLayout"
        ) {
            return theme.frameLayoutIcon
        }

        if (nodeTypeShort == "AppBarLayout" || node.name == "com.android.internal.widget.ActionBarContainer") {
            return theme.appBarIcon
        }

        if (nodeTypeShort == "ConstraintLayout") {
            return theme.constraintLayoutIcon
        }

        if (nodeTypeShort == "CollapsingToolbarLayout" || nodeTypeShort == "Toolbar") {
            return theme.toolbarIcon
        }

        if (nodeTypeShort == "CoordinatorLayout") {
            return theme.coordinatorLayoutIcon
        }

        if (nodeTypeShort == "AppCompatImageButton" || nodeTypeShort == "ImageButton" || nodeTypeShort == "ImageView") {
            return theme.imageViewIcon
        }

        if (nodeTypeShort == "ViewSwitcher") {
            return theme.viewSwitcherIcon
        }

        if (nodeTypeShort == "NestedScrollView") {
            return theme.nestedScrollViewIcon
        }

        if (nodeTypeShort.contains("RecyclerView")) {
            return theme.listViewIcon
        }

        if (nodeTypeShort.contains("RelativeLayout")) {
            return theme.relativeLayoutIcon
        }

        if (nodeTypeShort.endsWith("CardView")) {
            return theme.cardViewIcon
        }
        if (nodeTypeShort.endsWith("ViewPager")) {
            return theme.viewPagerIcon
        }
        if (nodeTypeShort == "FloatingActionButton") {
            return theme.fabIcon
        }
        if (nodeTypeShort.endsWith("LinearLayout")) {
            return theme.linearLayoutIcon
        }
        return theme.viewIcon
    }

}
