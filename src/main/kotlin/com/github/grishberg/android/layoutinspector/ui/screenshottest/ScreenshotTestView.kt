package com.github.grishberg.android.layoutinspector.ui.screenshottest

interface ScreenshotTestView {
    fun showNoDifferences()

    fun showHasDifferences(differencesCount: Int)
}