package com.github.grishberg.android.layoutinspector.ui.info.flat.filter

import java.awt.Component

interface FilterView {
    val component: Component

    val filterText: String

    fun setOnTextChangedListener(listener: (String) -> Unit)
}