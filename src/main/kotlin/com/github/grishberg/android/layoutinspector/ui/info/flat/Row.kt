package com.github.grishberg.android.layoutinspector.ui.info.flat

import com.github.grishberg.android.layoutinspector.ui.info.RowInfoImpl

sealed class Row {
    class ValueRow(val property: RowInfoImpl) : Row()

    class HeaderRow(val name: String) : Row()
}