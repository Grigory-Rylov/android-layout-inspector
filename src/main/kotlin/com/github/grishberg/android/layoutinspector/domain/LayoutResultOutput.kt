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
}
