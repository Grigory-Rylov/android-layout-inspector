package com.github.grishberg.android.layoutinspector.compose

import com.github.grishberg.android.layoutinspector.compose.ComposeInspectionCodec.ComposeParameter
import com.github.grishberg.android.layoutinspector.compose.ComposeInspectionCodec.ParameterGroup

/**
 * Pulls the visible text out of decoded parameter groups (`get_all_parameters_command`).
 *
 * A `Text` composable exposes its value through the `androidx.compose.ui.semantics.Text`
 * semantics property, a `List<AnnotatedString>`. The compose agent serializes semantics as named
 * `Parameter`s: the property name goes into `name` (string table), list values are `ITERABLE`
 * parameters whose STRING elements carry the string itself in their own `name` field (the agent's
 * serializer has no separate string-value field). Merged semantics are preferred; a node without
 * merged semantics falls back to unmerged ones.
 */
object ComposeTextExtractor {

    /** Parameter.Type values from compose_layout_inspection.proto. */
    const val TYPE_STRING = 1
    const val TYPE_ITERABLE = 14

    /** SemanticsProperties.Text.name - the property holding a Text composable's value. */
    const val TEXT_SEMANTIC_NAME = "Text"

    /** @return composable id -> visible text, for every group with a non-empty `Text` semantics. */
    fun extract(groups: List<ParameterGroup>): Map<Long, String> {
        val result = LinkedHashMap<Long, String>()
        for (group in groups) {
            val text = textOf(group) ?: continue
            result[group.composableId] = text
        }
        return result
    }

    /** Merged-first lookup of the `Text` semantics property value of a single composable. */
    fun textOf(group: ParameterGroup): String? {
        val property = group.mergedSemantics.firstOrNull { it.name == TEXT_SEMANTIC_NAME }
            ?: group.unmergedSemantics.firstOrNull { it.name == TEXT_SEMANTIC_NAME }
            ?: return null
        // The Text semantics property is a List<AnnotatedString>; for property parameters `name`
        // is the property key, so only ITERABLE values carry a usable string.
        return if (property.type == TYPE_ITERABLE) valueAsString(property) else null
    }

    /** Flattens a STRING or ITERABLE-of-STRING parameter value; null for anything else. */
    internal fun valueAsString(parameter: ComposeParameter): String? = when (parameter.type) {
        TYPE_STRING -> parameter.name.ifEmpty { null }
        TYPE_ITERABLE -> {
            val joined = parameter.elements.mapNotNull { leaf ->
                when (leaf.type) {
                    TYPE_STRING -> leaf.name
                    TYPE_ITERABLE -> valueAsString(leaf)
                    else -> null
                }
            }.joinToString(separator = "")
            joined.ifEmpty { null }
        }
        else -> null
    }
}
