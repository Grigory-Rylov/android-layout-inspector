package com.github.grishberg.android.layoutinspector.ui.tree


import com.github.grishberg.android.layoutinspector.ui.theme.ThemeColors
import java.awt.Color
import java.awt.Dimension
import java.awt.FontMetrics
import java.awt.Graphics
import javax.swing.Icon
import javax.swing.ImageIcon
import javax.swing.JPanel
import javax.swing.UIManager
import javax.swing.plaf.UIResource
import kotlin.math.max

/**
 * Renderer for TextView item with text component.
 */
class ItemViewRenderer(
    theme: ThemeColors
) : JPanel(), TreeItem {
    // Icons
    /** Icon used to show non-leaf nodes that aren't expanded.  */
    private var closedIcon: Icon? = null

    /** Icon used to show non-leaf nodes that are expanded.  */
    private var openIcon: Icon? = null
    private var treeNodeIcon: Icon? = null
    private var leafWidth = 0

    private var icon: ImageIcon = ImageIcon()
    private var selectionBackground: Color = theme.selectionBackground

    private var foreground1 = Color.BLACK
    private var foreground2 = Color.BLACK

    private var text1: String = ""
    private var text2: String = ""
    private val iconGap = 4
    private val textRightPadding = 4
    private val fontMetrics: FontMetrics = getFontMetrics(font)
    private var selected: Boolean = false

    var nodeVisible: Boolean = true
    private var hovered = false

    /**
     * Set to true after the constructor has run.
     */
    private val initiated: Boolean = true

    override fun setForeground(textColor1: Color, textColor2: Color) {
        foreground1 = textColor1
        foreground2 = textColor2
    }

    override fun setBackgroundSelectionColor(color: Color) {
        this.selectionBackground = color
    }

    override fun setIcon(newIcon: ImageIcon) {
        icon = newIcon
    }

    override fun prepareTreeItem(
        type: String, description: String,
        sel: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        hovered: Boolean,
        hasFocus: Boolean
    ) {
        selected = sel
        this.hovered = hovered
        treeNodeIcon = when {
            leaf -> null
            expanded -> openIcon
            else -> closedIcon
        }

        setTitle(type, description)
    }

    private fun setTitle(t1: String, t2: String) {
        val textChanged = t1 != text1 || t2 != text2

        text1 = t1
        if (t2.isNotEmpty()) {
            text2 = " - \"$t2\""
        } else {
            text2 = ""
        }
        if (textChanged) {
            preferredSize = calculateDimension(text1, text2)
            revalidate()
            repaint()
        }
    }

    private fun calculateDimension(t1: String, t2: String): Dimension {
        var w = iconGap + icon.iconWidth + iconGap + fontMetrics.stringWidth(t1) +
                fontMetrics.stringWidth(t2) + textRightPadding
        if (leafWidth > 0) {
            w += leafWidth + iconGap
        }
        return Dimension(w, max(icon.iconHeight, fontMetrics.height))
    }

    override fun paintComponent(g: Graphics) {
        var left = iconGap
        val currentNodeIcon = treeNodeIcon
        if (currentNodeIcon != null) {
            val treeNodeIconTop = (height - currentNodeIcon.iconHeight) / 2
            currentNodeIcon.paintIcon(this, g, left, treeNodeIconTop)
            left += iconGap + currentNodeIcon.iconWidth
        } else if (leafWidth > 0) {
            left += iconGap + leafWidth
        }

        val iconTop = (height - icon.iconHeight) / 2

        g.drawImage(icon.image, left, iconTop, this)
        left += icon.iconWidth + iconGap

        val textHeight = fontMetrics.ascent - fontMetrics.descent - fontMetrics.leading
        val textTop = textHeight + (height - textHeight) / 2

        g.color = foreground1
        g.drawString(text1, left, textTop)
        left += fontMetrics.stringWidth(text1)

        if (text2.isNotEmpty()) {
            g.color = foreground2
            g.drawString(text2, left, textTop)
        }
    }

    override fun updateUI() {
        super.updateUI()
        // To avoid invoking new methods from the constructor, the
        // initiated field is first checked. If initiated is false, the constructor
        // has not run and there is no point in checking the value. As
        // all look and feels have a non-null value for these properties,
        // a null value means the developer has specifically set it to
        // null. As such, if the value is null, this does not reset the
        // value.
        if (!initiated || closedIcon is UIResource) {
            closedIcon = UIManager.getIcon("Tree.closedIcon")
            closedIcon?.let {
                leafWidth = it.iconWidth
            }
        }
        if (!initiated || openIcon is UIManager) {
            openIcon = UIManager.getIcon("Tree.openIcon")
        }
    }
}
