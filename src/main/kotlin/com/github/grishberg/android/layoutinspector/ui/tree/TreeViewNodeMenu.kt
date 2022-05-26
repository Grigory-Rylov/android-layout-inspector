package com.github.grishberg.android.layoutinspector.ui.tree

import com.android.layoutinspector.model.ViewNode
import com.github.grishberg.android.layoutinspector.domain.MetaRepository
import com.github.grishberg.android.layoutinspector.ui.common.createControlAccelerator
import com.github.grishberg.android.layoutinspector.ui.common.createControlAltAccelerator
import com.github.grishberg.android.layoutinspector.ui.dialogs.bookmarks.Bookmarks
import com.github.grishberg.android.layoutinspector.ui.dialogs.bookmarks.NewBookmarkDialog
import javax.swing.JFrame
import javax.swing.JMenuItem
import javax.swing.JPopupMenu

typealias CalculateDistanceDelegate = () -> Unit

class TreeViewNodeMenu(
    val owner: JFrame,
    val treePanel: TreePanel,
    val selectedViewNode: ViewNode,
    val meta: MetaRepository,
    val bookmarks: Bookmarks,
    val calculateDistanceDelegate: CalculateDistanceDelegate?
) : JPopupMenu() {
    private val addToBookmark = JMenuItem("Add to bookmarks")
    private val editBookmark = JMenuItem("Edit bookmark")
    private val deleteBookmark = JMenuItem("Delete bookmark")
    private val calculateDistance = JMenuItem("Calculate distance")
    private val hideView = JMenuItem("Hide from layout")
    private val removeFromHidden = JMenuItem("Show on layout")
    private val copyId = JMenuItem("Copy id").apply {
        accelerator = createControlAltAccelerator('C')
    }
    private val copyShortClassName = JMenuItem("Copy short class name").apply {
        accelerator = createControlAccelerator('C')
    }

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
            }

            add(deleteBookmark)
            deleteBookmark.addActionListener {
                bookmarks.remove(existingBookmarkInfo)
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

        copyId.addActionListener {
            treePanel.copyIdToClipboard()
        }
        copyShortClassName.addActionListener {
            treePanel.copyShortNameToClipboard()
        }
        add(copyId)
        add(copyShortClassName)
    }
}
