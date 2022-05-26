package com.github.grishberg.android.layoutinspector.ui.info

import com.android.layoutinspector.model.ViewProperty
import java.math.RoundingMode
import java.text.DecimalFormat

/**
 * Table row value model.
 */
data class RowInfoImpl(
    private val property: ViewProperty,
    private val sizeInDp: Boolean,
    private val dpPerPixels: Double
) {

    fun name() = property.name

    fun value(): String {

        if (sizeInDp && property.isSizeProperty && dpPerPixels > 1) {
            return roundOffDecimal(property.intValue.toDouble() / dpPerPixels) + " dp"
        }
        return property.value
    }

    override fun toString(): String {
        return property.name
    }

    private fun roundOffDecimal(number: Double): String {
        val df = DecimalFormat("#.##")
        df.roundingMode = RoundingMode.CEILING
        return df.format(number)
    }
}