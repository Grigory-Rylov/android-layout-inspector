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
class TextViewRenderer(
    private val icon: ImageIcon,
    theme: ThemeColors
) : JPanel(), TreeItem {
    private val selectionBackground: Color = theme.selectionBackground

    private var foreground1 = Color.BLACK
    private var foreground2 = Color.BLACK

    private var text1: String = ""
    private var text2: String = ""
    private val iconGap = 4
    private val fontMetrics: FontMetrics = getFontMetrics(font)

    var nodeVisible: Boolean = true
    var selected: Boolean = false
    var hovered = false


    override fun setTitle(t1: String, t2: String) {
        text1 = t1
        text2 = " - \"$t2\""
        preferredSize = calculateDimension(text1, text2)
        repaint()
    }

    private fun calculateDimension(t1: String, t2: String): Dimension {
        val w = icon.iconWidth + iconGap + fontMetrics.stringWidth(t1) + fontMetrics.stringWidth(t2)
        return Dimension(w, max(icon.iconHeight, fontMetrics.height))
    }

    override fun setForeground(textColor1: Color, textColor2: Color) {
        foreground1 = textColor1
        foreground2 = textColor2
    }

    override fun setIcon(icon: ImageIcon) = Unit

    override fun paintComponent(g: Graphics) {
        var left = 0
        val iconTop = (height - icon.iconHeight) / 2
        g.drawImage(icon.image, 0, iconTop, this)
        left += icon.iconWidth + iconGap

        if (selected) {
            g.color = selectionBackground
            val textWidth = fontMetrics.stringWidth(text1) + fontMetrics.stringWidth(text2)
            g.fillRect(left, 0, textWidth, height)
        }

        val textHeight = fontMetrics.ascent - fontMetrics.descent - fontMetrics.leading
        val textTop = textHeight + (height - textHeight) / 2

        g.color = foreground1
        g.drawString(text1, left, textTop)
        left += fontMetrics.stringWidth(text1)

        g.color = foreground2
        g.drawString(text2, left, textTop)
    }
}
