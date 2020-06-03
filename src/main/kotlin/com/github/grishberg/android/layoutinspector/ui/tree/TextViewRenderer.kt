package com.github.grishberg.android.layoutinspector.ui.tree


import java.awt.Color
import java.awt.Dimension
import java.awt.FontMetrics
import java.awt.Graphics
import javax.swing.ImageIcon
import javax.swing.JPanel
import javax.swing.UIManager
import kotlin.math.max

class TextViewRenderer(
    private val icon: ImageIcon
) : JPanel() {
    private val selectionBorderColor: Color = UIManager.getColor("Tree.selectionBorderColor")
    private val selectionForeground: Color = UIManager.getColor("Tree.selectionForeground")
    private val selectionBackground: Color = UIManager.getColor("Tree.selectionBackground")
    private val textForeground: Color = UIManager.getColor("Tree.textForeground")
    private val textBackground: Color = UIManager.getColor("Tree.textBackground")

    private val text2Foreground: Color = Color(0, 0, 0, 127)
    private val text2SelectedForeground: Color = Color(0, 0, 0, 127)
    private val hoveredText1Color = Color(45, 71, 180)
    private val hoveredText2Color = Color(57, 90, 227)

    private val selectionHoveredText1Color = Color(186, 225, 238)
    private val selectionHoveredText2Color = Color(186, 225, 238, 127)

    private val hiddenText1Color = Color(0, 0, 0, 127)
    private val hiddenText2Color = Color(0, 0, 0, 90)

    private val selectionHiddenTextColor = Color(220, 220, 220)

    private var text1: String = ""
    private var text2: String = ""
    private val iconGap = 4
    private val fontMetrics: FontMetrics = getFontMetrics(font)

    var nodeVisible: Boolean = true
    var selected: Boolean = false
    var hovered = false

    fun setText(t1: String, t2: String) {
        text1 = t1
        text2 = " - \"$t2\""
        preferredSize = calculateDimension(text1, text2)
        repaint()
    }

    private fun calculateDimension(t1: String, t2: String): Dimension {
        val w = icon.iconWidth + iconGap + fontMetrics.stringWidth(t1) + fontMetrics.stringWidth(t2)
        return Dimension(w, max(icon.iconHeight, fontMetrics.height))
    }

    override fun paintComponent(g: Graphics) {

        var left = 0
        g.drawImage(icon.image, 0, 0, this)
        left += icon.iconWidth + iconGap

        if (selected) {
            g.color = selectionBackground
            val textWidth = fontMetrics.stringWidth(text1) + fontMetrics.stringWidth(text2)
            g.fillRect(left, 0, textWidth, height)
        }

        val text1Foreground = text1Foreground()
        val text2Foreground = text2Foreground()

        val textHeight = fontMetrics.ascent - fontMetrics.descent - fontMetrics.leading
        val textTop = textHeight + (icon.iconHeight - textHeight) / 2

        g.color = text1Foreground
        g.drawString(text1, left, textTop)
        left += fontMetrics.stringWidth(text1)

        g.color = text2Foreground
        g.drawString(text2, left, textTop)
    }

    private fun text1Foreground(): Color {
        if (hovered) {
            if (selected) {
                return selectionHoveredText1Color
            }
            return hoveredText1Color
        }
        if (!nodeVisible) {
            if (selected) {
                return selectionHiddenTextColor
            }
            return hiddenText1Color
        }
        if (selected) {
            return selectionForeground
        }

        return textForeground
    }

    private fun text2Foreground(): Color {
        if (hovered) {
            if (selected) {
                return selectionHoveredText2Color
            }
            return hoveredText2Color
        }
        if (!nodeVisible) {
            if (selected) {
                return selectionHiddenTextColor
            }
            return hiddenText2Color
        }
        if (selected) {
            return text2SelectedForeground
        }

        return text2Foreground
    }
}