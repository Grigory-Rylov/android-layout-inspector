package com.github.grishberg.android.layoutinspector.ui.layout

import com.android.layoutinspector.common.AppLogger
import java.awt.Toolkit
import java.awt.image.BufferedImage

class ScreenshotClipboardManager(
    private val imageHelper: ImageHelper,
    private val logger: AppLogger,
) {
    fun copyToClipboard(image: BufferedImage?) {
        if (image == null) {
            return
        }
        try {
            val newImage = imageHelper.copyForClipboard(image)
            val t = ImageTransferable(newImage)
            Toolkit.getDefaultToolkit().systemClipboard
                .setContents(t, null)
        } catch (e: Exception) {
            logger.e("Can't copy image to clipboard", e)
        }
    }
}