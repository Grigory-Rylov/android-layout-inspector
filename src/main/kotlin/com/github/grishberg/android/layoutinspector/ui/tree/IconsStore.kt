package com.github.grishberg.android.layoutinspector.ui.tree

import java.awt.Image
import javax.swing.ImageIcon

private const val ICON_SIZE = 16

class IconsStore {
    fun createImageIcon(path: String): ImageIcon {
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