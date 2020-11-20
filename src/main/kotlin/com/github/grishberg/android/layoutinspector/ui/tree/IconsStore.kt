package com.github.grishberg.android.layoutinspector.ui.tree

import com.intellij.openapi.util.IconLoader
import com.intellij.util.IconUtil
import com.intellij.util.ui.ImageUtil
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import org.imgscalr.Scalr
import java.awt.image.BufferedImage
import javax.swing.ImageIcon


private const val ICON_SIZE = 18

class IconsStore(private val iconSize: Int = ICON_SIZE) {
    fun createImageIcon(path: String, altText: String = ""): ImageIcon {
        val icon = IconLoader.getIcon(path)

        var image: BufferedImage? = ImageUtil.toBufferedImage(IconUtil.toImage(icon))
        image =
            Scalr.resize(image, Scalr.Method.ULTRA_QUALITY, if (UIUtil.isRetina()) iconSize else JBUI.scale(iconSize))
        return if (image != null) {
            ImageIcon(image, altText)
        } else {
            throw IllegalStateException("Image $path not found")
        }
    }
}
