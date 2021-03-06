package com.github.grishberg.android.layoutinspector.ui.tree

import com.android.layoutinspector.model.ViewNode
import com.github.grishberg.android.layoutinspector.domain.MetaRepository
import com.github.grishberg.android.layoutinspector.ui.dialogs.bookmarks.Bookmarks
import com.github.grishberg.android.layoutinspector.ui.dialogs.bookmarks.NewBookmarkDialog
import javax.swing.JFrame
import javax.swing.JMenuItem
import javax.swing.JPopupMenu

typealias CalculateDistanceDelegate = () -> Unit

class TreeViewNodeMenu(
    val owner: JFrame,
    val selectedViewNode: ViewNode,
    val meta: MetaRepository,
    val bookmarks: Bookmarks,
    val calculateDistanceDelegate: CalculateDistanceDelegate?
) : JPopupMenu() {
    private val addToBookmark = JMenuItem("Add to bookmarks")
    private val editBookmark = JMenuItem("Edit bookmark")
    private val calculateDistance = JMenuItem("Calculate distance")
    private val hideView = JMenuItem("Hide from layout")
    private val removeFromHidden = JMenuItem("Show on layout")

    init {
        addToBookmark.addActionListener {
            val bookmarksDialog = NewBookmarkDialog(owner, selectedViewNode)
            bookmarksDialog.showDialog()

            bookmarksDialog.result?.let {
                bookmarks.add(it)
            }
        }

        val existingBookmarkInfo = bookmarks.getBookmarkInfoForNode(selectedViewNode)
        if (existingBookmarkInfo == null) {
            add(addToBookmark)
        } else {
            add(editBookmark)
            editBookmark.addActionListener {
                val bookmarksDialog = NewBookmarkDialog(owner, selectedViewNode)
                bookmarksDialog.showEditDialog(existingBookmarkInfo)

                bookmarksDialog.result?.let {
                    bookmarks.edit(existingBookmarkInfo, it)
                }
                bookmarks
            }

        }

        if (meta.shouldHideInLayout(selectedViewNode)) {
            add(removeFromHidden)
            removeFromHidden.addActionListener {
                meta.removeFromHiddenViews(selectedViewNode)

            }
        } else {
            add(hideView)
            hideView.addActionListener {
                meta.addToHiddenViews(selectedViewNode)
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