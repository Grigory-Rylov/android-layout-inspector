package com.github.grishberg.android.layoutinspector.ui.dialogs.bookmarks

import com.github.grishberg.android.layoutinspector.domain.AbstractViewNode
import java.awt.Color

data class BookmarkInfo(
    val node: AbstractViewNode,
    var description: String?,
    var color: Color?
)
