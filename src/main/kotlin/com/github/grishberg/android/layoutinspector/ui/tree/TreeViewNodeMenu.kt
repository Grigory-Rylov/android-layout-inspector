package com.github.grishberg.android.layoutinspector.ui.tree

import com.android.layoutinspector.model.ViewNode
import com.github.grishberg.android.layoutinspector.ui.dialogs.bookmarks.Bookmarks
import com.github.grishberg.android.layoutinspector.ui.dialogs.bookmarks.NewBookmarkDialog
import javax.swing.JFrame
import javax.swing.JMenuItem
import javax.swing.JPopupMenu

typealias CalculateDistanceDelegate = () -> Unit

class TreeViewNodeMenu(
    val owner: JFrame,
    val selectedViewNode: ViewNode,
    val bookmarks: Bookmarks,
    val calculateDistanceDelegate: CalculateDistanceDelegate?
) : JPopupMenu() {
    private val addToBookmark = JMenuItem("Add to bookmarks")
    private val editBookmark = JMenuItem("Edit bookmark")
    private val calculateDistance = JMenuItem("Calculate distance")

    init {
        add(addToBookmark)
        addToBookmark.addActionListener {
            val bookmarksDialog = NewBookmarkDialog(owner, selectedViewNode)
            bookmarksDialog.showDialog()

            bookmarksDialog.result?.let {
                bookmarks.add(it)
            }
        }

        val existingBookmarkInfo = bookmarks.getBookmarkInfoForNode(selectedViewNode)
        existingBookmarkInfo?.let { bookmarkInfo ->

            add(editBookmark)
            editBookmark.addActionListener {
                val bookmarksDialog = NewBookmarkDialog(owner, selectedViewNode)
                bookmarksDialog.showEditDialog(bookmarkInfo)

                bookmarksDialog.result?.let {
                    bookmarks.edit(bookmarkInfo, it)
                }

                bookmarks
            }

        }

        calculateDistanceDelegate?.let { delegate ->
            add(calculateDistance)
            calculateDistance.addActionListener {
                delegate.invoke()
            }
        }
    }
}