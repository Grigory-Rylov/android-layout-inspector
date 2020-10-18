package com.github.grishberg.android.layoutinspector.ui.dialogs.bookmarks

import com.android.layoutinspector.model.ViewNode
import java.awt.Color

data class BookmarkInfo(
    val node: ViewNode,
    var description: String?,
    var color: Color?
)