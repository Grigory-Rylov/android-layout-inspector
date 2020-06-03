package com.github.grishberg.android.layoutinspector.ui.tree

import com.android.layoutinspector.model.ViewNode
import java.awt.Color
import java.awt.Component
import java.awt.Image
import javax.swing.ImageIcon
import javax.swing.JTree
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.TreeCellRenderer

private const val ICON_SIZE = 16

class NodeViewTreeCellRenderer : TreeCellRenderer {
    var hoveredNode: ViewNode? = null

    val fabIcon = createImageIcon("icons/fab.png")
    val appBarIcon = createImageIcon("icons/appbar.png")
    val coordinatorLayoutIcon = createImageIcon("icons/coordinator_layout.png")
    val constraintLayoutIcon = createImageIcon("icons/constraint_layout.png")
    val frameLayoutIcon = createImageIcon("icons/frame_layout.png")
    val linearLayoutIcon = createImageIcon("icons/linear_layout.png")
    val cardViewIcon = createImageIcon("icons/cardView.png")
    val viewStubIcon = createImageIcon("icons/viewstub.png")
    val toolbarIcon = createImageIcon("icons/toolbar.png")
    val listViewIcon = createImageIcon("icons/recyclerView.png")
    val relativeLsyoutIcon = createImageIcon("icons/relativeLayout.png")
    val textIcon = createImageIcon("icons/text.png")
    val imageViewIcon = createImageIcon("icons/imageView.png")
    val nestedScrollViewIcon = createImageIcon("icons/nestedScrollView.png")
    val viewSwitcherIcon = createImageIcon("icons/viewSwitcher.png")
    val viewPagerIcon = createImageIcon("icons/viewPager.png")
    val viewIcon = createImageIcon("icons/view.png")

    private val hoveredTextColor = Color(45, 71, 180)
    private val selectionHoveredText1Color = Color(220, 225, 238)
    private val hiddenTextColor = Color(0, 0, 0, 127)
    private val selectionHiddenTextColor = Color(220, 220, 220)

    private val textViewRenderer = TextViewRenderer(textIcon)
    private val defaultCellRenderer = DefaultTreeCellRenderer()

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

        defaultCellRenderer.text = value.getFormattedName()

        val text = value.getText()
        if (text != null) {
            textViewRenderer.setText(value.typeAsString(), value.getElliptizedText(text))
            textViewRenderer.selected = selected
            textViewRenderer.hovered = hovered
            return textViewRenderer
        }

        defaultCellRenderer.icon = iconForNode(value)

        if (!visible) {
            if (selected) {
                defaultCellRenderer.foreground = selectionHiddenTextColor
            } else {
                defaultCellRenderer.foreground = hiddenTextColor
            }
        }
        if (hovered) {
            if (selected) {
                defaultCellRenderer.foreground = selectionHoveredText1Color
            } else {
                defaultCellRenderer.foreground = hoveredTextColor
            }
        }
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

        if (nodeTypeShort == "AppBarLayout") {
            return appBarIcon
        }

        if (nodeTypeShort == "ConstraintLayout") {
            return constraintLayoutIcon
        }

        if (nodeTypeShort == "CollapsingToolbarLayout") {
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

    /** Returns an ImageIcon, or null if the path was invalid.  */
    private fun createImageIcon(path: String): ImageIcon {
        val imgURL = ClassLoader.getSystemResource(path)
        return if (imgURL != null) {
            val readedIcon = ImageIcon(imgURL)
            val image: Image = readedIcon.getImage()
            ImageIcon(image.getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_SMOOTH))
        } else {
            throw IllegalStateException("Image $path not found")
        }
    }
}