package com.github.grishberg.android.layoutinspector.ui.tree

import com.github.grishberg.android.layoutinspector.domain.AbstractViewNode
import com.github.grishberg.android.layoutinspector.ui.dialogs.bookmarks.Bookmarks
import com.github.grishberg.android.layoutinspector.ui.theme.ThemeColors
import com.intellij.ui.JBColor
import java.awt.Color
import java.awt.Component
import javax.swing.ImageIcon
import javax.swing.JTree
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.TreeCellRenderer

private const val MAX_TEXT_LENGTH = 20

class NodeViewTreeCellRenderer(
    private val foundItems: List<AbstractViewNode>,
    private val theme: ThemeColors,
    private val bookmarks: Bookmarks
) : TreeCellRenderer {
    var hoveredNode: AbstractViewNode? = null

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
        if (value !is AbstractViewNode) {
            return DefaultTreeCellRenderer()
        }
        val hovered = value == hoveredNode
        val visible = value.isVisible
        val text = value.text
        val highlighted = foundItems.contains(value)

        if (selected) {
            itemRenderer.setBackgroundSelectionColor(theme.selectionBackground)
        } else if (hovered) {
            itemRenderer.setBackgroundSelectionColor(theme.hoverBackground)
        }

        val defaultTextForeground = text1Foreground.textForeground(selected, hovered, highlighted, visible)
        val foreground1 = bookmarks.getForegroundForItem(value, defaultTextForeground)

        val foreground2 = text2Foreground.textForeground(selected, hovered, highlighted, visible)
        itemRenderer.setForeground(foreground1, foreground2)


        if (!text.isNullOrEmpty()) {
            itemRenderer.setIcon(theme.textIcon)
            itemRenderer.prepareTreeItem(
                value.typeAsString,
                getElliptizedText(text),
                selected,
                expanded,
                leaf,
                hovered
            )
        } else {
            itemRenderer.setIcon(iconForNode(value))
            itemRenderer.prepareTreeItem(getFormattedName(value.id, text, value.typeAsString, ), "", selected, expanded, leaf, hovered)
        }

        return itemRenderer
    }

    private fun createSelectedColor(color: Color) = lighter(color, 0.5f)

    private fun titleColor(colorWithoutAlpha: Color): Color {
        val y =
            (299 * colorWithoutAlpha.red + 587 * colorWithoutAlpha.green + 114 * colorWithoutAlpha.blue) / 1000.toDouble()
        return if (y >= 128) JBColor.black else JBColor.white
    }


    private fun iconForNode(node: AbstractViewNode): ImageIcon {
        val nodeTypeShort = node.typeAsString

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

        if (nodeTypeShort == "ComposeView") {
            return theme.composeIcon
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

    fun lighter(color: Color, ratio: Float): Color {
        return mergeColors(Color.WHITE, ratio, color, 1 - ratio)
    }

    /**
     * Merges two colors. The two floating point arguments specify "how much" of the corresponding color is added to the
     * resulting color. Both arguments should (but don't have to) add to `1.0`.
     *
     *
     * This method is null-safe. If one of the given colors is `null`, the other color is returned (unchanged).
     */
    fun mergeColors(a: Color, fa: Float, b: Color, fb: Float): Color {

        return Color(
            (fa * a.red + fb * b.red) / (fa + fb) / 255f,
            (fa * a.green + fb * b.green) / (fa + fb) / 255f,
            (fa * a.blue + fb * b.blue) / (fa + fb) / 255f
        )
    }

    fun getElliptizedText(text: String): String {
        if (text.length <= MAX_TEXT_LENGTH) {
            return text
        }
        return text.substring(0, MAX_TEXT_LENGTH) + "…"
    }

    fun getFormattedName(id: String?, text: String?, typeAsString: String): String {
        val idPrefix = if (id != null && id != "NO_ID") id else null

        if (text != null) {
            if (idPrefix != null) {
                return "$idPrefix ($typeAsString) - \"$text\""
            }
            return "$typeAsString - \"$text\""
        }
        if (idPrefix != null) {
            return "$idPrefix ($typeAsString)"
        }
        return typeAsString
    }

}
