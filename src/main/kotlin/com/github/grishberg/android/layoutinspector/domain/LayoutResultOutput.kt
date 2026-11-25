package com.github.grishberg.android.layoutinspector.domain

import com.android.layoutinspector.model.LayoutFileData

interface LayoutResultOutput {

    /**
     * Show hierarchy and screenshot
     */
    fun showResult(resultOutput: LayoutFileData, label: String? = null, isRefresh: Boolean = false)

    fun showError(error: String)

    fun showLoading()

    fun hideLoading()

    /**
     * Show (or update the text of) a progress indicator for the background Jetpack Compose
     * capture phase. Must be called on the EDT.
     */
    fun showComposeProgress(text: String)

    /**
     * Hide the Jetpack Compose progress indicator. Safe to call when nothing is shown.
     * Must be called on the EDT.
     */
    fun hideComposeProgress()
}
