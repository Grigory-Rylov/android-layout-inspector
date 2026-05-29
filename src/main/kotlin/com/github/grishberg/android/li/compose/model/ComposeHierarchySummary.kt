package com.github.grishberg.android.li.compose.model

data class ComposeHierarchySummary(
    val deviceName: String,
    val packageName: String,
    val totalNodes: Int,
    val visibleNodes: Int,
    val composeRootCount: Int,
    val composeCandidateCount: Int,
    val textNodeCount: Int,
    val contentDescNodeCount: Int,
    val resourceIdNodeCount: Int,
    val classDistribution: List<ClassCount>,
    val topTexts: List<String>,
    val topContentDescriptions: List<String>,
) {
    data class ClassCount(val className: String, val count: Int)
}
