package com.github.grishberg.android.layoutinspector.ui.screenshottest

import java.awt.image.BufferedImage

class ScreenshotTestLogic(
    private val view: ScreenshotTestView,
) {
    fun compare(
        reference: BufferedImage,
        comparable: BufferedImage,
        screenshotPainter: ScreenshotPainter
    ) {
        var differencesCount = 0
        for (x in 0 until reference.width) {
            for (y in 0 until comparable.height) {
                if (reference.getRGB(x, y) != comparable.getRGB(x, y)) {
                    differencesCount++
                    screenshotPainter.paintDifferencePixel(x, y)
                }
            }
        }

        screenshotPainter.invalidate()

        if (differencesCount == 0){
            view.showNoDifferences()
        } else {
            view.showHasDifferences(differencesCount)
        }
    }
}