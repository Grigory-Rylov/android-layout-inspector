package com.github.grishberg.android.layoutinspector.ui.tree

import com.intellij.openapi.util.IconLoader
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import javax.swing.Icon
import javax.swing.ImageIcon


private const val ICON_SIZE = 18

class IconsStore(private val iconSize: Int = ICON_SIZE) {
    fun createImageIcon(path: String, altText: String = ""): ImageIcon {
        val icon = IconLoader.getIcon(path, this.javaClass)

        return resizeIcon(icon, iconSize, iconSize)
    }

    private fun resizeIcon(icon: Icon, width: Int, height: Int): ImageIcon {
        val bufferedImage = BufferedImage(
            icon.iconWidth, icon.iconHeight,
            BufferedImage.TYPE_4BYTE_ABGR
        )
        val bufImageG: Graphics2D = bufferedImage.createGraphics()
        icon.paintIcon(null, bufImageG, 0, 0)
        bufImageG.dispose()

        val resizedImg = BufferedImage(
            width, height,
            BufferedImage.TYPE_4BYTE_ABGR
        )

        val g2: Graphics2D = resizedImg.createGraphics()
        g2.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BILINEAR
        )
        g2.drawImage(bufferedImage, 0, 0, width, height, null)
        g2.dispose()

        return ImageIcon(resizedImg)
    }
}
