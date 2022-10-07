package com.github.grishberg.android.layoutinspector.ui.layout

import java.awt.Graphics2D
import java.awt.GraphicsEnvironment
import java.awt.Image
import java.awt.Transparency
import java.awt.image.BufferedImage


class ImageHelper {
    private val GFX_CONFIG = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration

    fun copyImage(source: BufferedImage?, shouldCopyTransparency: Boolean = true): BufferedImage? {
        if (source == null) {
            return null
        }
        val newImage = GFX_CONFIG.createCompatibleImage(
            source.width,
            source.height,
            if (shouldCopyTransparency) source.transparency else Transparency.OPAQUE
        )
        // get the graphics context of the new image to draw the old image on
        val g2d = newImage.graphics as Graphics2D


        g2d.drawImage(source, 0, 0, null)
        g2d.dispose()
        return newImage
    }

    fun copyForClipboard(source: BufferedImage): BufferedImage {
        val original: Image = source
        val newImage = BufferedImage(
            original.getWidth(null), original.getHeight(null), BufferedImage.TYPE_INT_RGB
        )
        val g = newImage.createGraphics()
        g.drawImage(original, 0, 0, null)
        g.dispose()
        return newImage
    }
}