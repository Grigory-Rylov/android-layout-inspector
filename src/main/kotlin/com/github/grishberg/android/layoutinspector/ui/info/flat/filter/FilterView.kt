package com.github.grishberg.android.layoutinspector.ui.info.flat.filter

import java.awt.Component

interface FilterView {
    val component: Component

    fun getFilterText(): String

    fun setOnTextChangedListener(listener: (String) -> Unit)
}