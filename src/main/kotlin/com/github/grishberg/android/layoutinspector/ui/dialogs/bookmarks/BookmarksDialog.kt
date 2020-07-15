package com.github.grishberg.android.layoutinspector.ui.dialogs.bookmarks

import com.github.grishberg.android.layoutinspector.ui.dialogs.CloseByEscapeDialog
import javax.swing.JFrame
import javax.swing.JTable

class BookmarksDialog(
    private val owner: JFrame
) : CloseByEscapeDialog(owner, "Bookmarks") {
    private val table: JTable

    init {
        table = JTable()
    }
}