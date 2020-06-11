package com.github.grishberg.android.layoutinspector.ui.tree


import com.github.grishberg.android.layoutinspector.ui.theme.ThemeColors
import java.awt.Color
import java.awt.Dimension
import java.awt.FontMetrics
import java.awt.Graphics
import javax.swing.ImageIcon
import javax.swing.JPanel
import kotlin.math.max

/**
 * Renderer for TextView item with text component.
 */
class ItemViewRenderer(
    theme: ThemeColors
) : JPanel(), TreeItem {
    private var icon: ImageIcon = ImageIcon()
    private var selectionBackground: Color = theme.selectionBackground

    private var foreground1 = Color.BLACK
    private var foreground2 = Color.BLACK

    private var text1: String = ""
    private var text2: String = ""
    private val iconGap = 4
    private val textRightPadding = 4
    private val fontMetrics: FontMetrics = getFontMetrics(font)

    var nodeVisible: Boolean = true
    override var selected: Boolean = false
    override var hovered = false
    override var leaf: Boolean = false
    override var expanded: Boolean = false

    override fun setTitle(t1: String, t2: String) {
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
        val w = iconGap + icon.iconWidth + iconGap + fontMetrics.stringWidth(t1) +
                fontMetrics.stringWidth(t2) + textRightPadding
        return Dimension(w, max(icon.iconHeight, fontMetrics.height))
    }

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

    override fun paintComponent(g: Graphics) {
        if (selected || hovered) {
            g.color = selectionBackground
            g.fillRect(0, 0, width, height)
        }

        var left = iconGap
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
}
