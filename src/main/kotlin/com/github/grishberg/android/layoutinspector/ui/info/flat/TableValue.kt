package com.github.grishberg.android.layoutinspector.ui.info.flat

import com.github.grishberg.android.layoutinspector.ui.info.RowInfoImpl

sealed class TableValue {
    class PropertyValue(val property: RowInfoImpl) : TableValue() {
        override fun toString(): String {
            return property.value()
        }
    }

    class PropertyName(val property: RowInfoImpl, val isSummary: Boolean = false) : TableValue() {
        override fun toString(): String {
            return property.name()
        }
    }

    class Header(val name: String) : TableValue() {
        override fun toString(): String {
            return name
        }
    }

    object Empty : TableValue() {
        override fun toString(): String = ""
    }
}