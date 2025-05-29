package com.github.grishberg.android.layoutinspector.settings

import com.github.grishberg.android.layoutinspector.domain.ViewNode

interface TreeSettings {
    var hideSystemNodes: Boolean
    var composeAsCallstack: Boolean
    var highlightSemantics: Boolean
    var supportLines: Boolean
    var showRecompositions: Boolean
    var highlightRecompositions: Boolean
    var ignoreRecompositionsInFramework: Boolean

    fun isInComponentTree(node: ViewNode): Boolean = !(hideSystemNodes && node.isSystemNode)
}

class TreeSettingsImpl : TreeSettings {
    override var hideSystemNodes: Boolean = true
    override var composeAsCallstack: Boolean = true
    override var highlightSemantics: Boolean = false
    override var supportLines: Boolean = true
    override var showRecompositions: Boolean = true
    override var highlightRecompositions: Boolean = true
    override var ignoreRecompositionsInFramework: Boolean = true
} 