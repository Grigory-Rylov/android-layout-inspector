package com.github.grishberg.android.layoutinspector.ui.screenshottest

interface ScreenshotPainter {
    fun paintDifferencePixel(x: Int, y: Int)

    fun invalidate()

    fun clearDifferences()
}