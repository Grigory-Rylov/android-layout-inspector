package com.github.grishberg.android.layoutinspector.ui.info.flat.filter

import com.github.grishberg.android.layoutinspector.ui.info.flat.FlatGroupTableModel
import com.github.grishberg.android.layoutinspector.ui.info.flat.TableValue
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.swing.RowFilter


class PropertiesTableFilter(
    text: String
) : RowFilter<FlatGroupTableModel, Int>() {


    private val regex: Pattern = Pattern.compile(text, Pattern.CASE_INSENSITIVE)
    private val matcher: Matcher = regex.matcher("");

    override fun include(entry: Entry<out FlatGroupTableModel, out Int>): Boolean {
        val item = entry.getValue(PROPERTY_NAME_COLUMN) as TableValue
        return when (item) {
            is TableValue.Header -> {
                return true
            }
            is TableValue.PropertyName -> {
                if (item.isSummary) {
                    return true
                }
                matcher.reset(item.toString())
                return matcher.find()
            }
            is TableValue.PropertyValue -> {
                return true
            }
            else -> true
        }
    }

    private companion object {
        private const val PROPERTY_NAME_COLUMN = 0
    }
}