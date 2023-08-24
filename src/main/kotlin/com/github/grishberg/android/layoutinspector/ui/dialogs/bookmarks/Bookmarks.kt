package com.github.grishberg.android.layoutinspector.ui.dialogs.bookmarks

import com.github.grishberg.android.layoutinspector.domain.AbstractViewNode
import java.awt.Color

typealias BookmarksChangedListener = () -> Unit

class Bookmarks {
    private val _items = mutableListOf<BookmarkInfo>()
    val listeners = mutableListOf<BookmarksChangedListener>()

    val items: List<BookmarkInfo>
        get() = _items

    fun add(newBookmark: BookmarkInfo) {
        _items.add(newBookmark)
        listeners.forEach {
            it.invoke()
        }
    }

    fun remove(item: BookmarkInfo) {
        _items.remove(item)
        listeners.forEach {
            it.invoke()
        }
    }

    fun edit(item: BookmarkInfo, newValue: BookmarkInfo) {
        item.color = newValue.color
        item.description = newValue.description
        listeners.forEach {
            it.invoke()
        }
    }

    fun find(text: String): BookmarkInfo? {
        return null
    }

    fun getForegroundForItem(value: AbstractViewNode, defaultTextForeground: Color): Color {
        var color = defaultTextForeground
        for (bookmarkInfo in _items) {
            if (bookmarkInfo.node != value) {
                continue
            }
            bookmarkInfo.color?.let {
                color = it
            }
            break
        }
        return color
    }

    fun getBookmarkInfoForNode(selectedViewNode: AbstractViewNode): BookmarkInfo? {
        for (bookmarkInfo in _items) {
            if (bookmarkInfo.node == selectedViewNode) {
                return bookmarkInfo
            }
        }
        return null
    }

    fun set(newItems: List<BookmarkInfo>) {
        _items.clear()
        _items.addAll(newItems)

        listeners.forEach {
            it.invoke()
        }
    }
}
