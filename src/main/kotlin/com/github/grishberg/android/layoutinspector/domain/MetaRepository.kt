package com.github.grishberg.android.layoutinspector.domain

import com.android.layoutinspector.common.AppLogger
import com.android.layoutinspector.model.ViewNode
import com.github.grishberg.android.layoutinspector.ui.common.colorToHex
import com.github.grishberg.android.layoutinspector.ui.common.hexToColor
import com.github.grishberg.android.layoutinspector.ui.dialogs.bookmarks.BookmarkInfo
import com.github.grishberg.android.layoutinspector.ui.dialogs.bookmarks.Bookmarks
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.*
import java.lang.reflect.Type

private const val TAG = "MetaRepository"
private const val META_DIR = "meta"

class MetaRepository(
    private val logger: AppLogger,
    private val bookmarks: Bookmarks
) {
    private val gson = GsonBuilder().enableComplexMapKeySerialization().setPrettyPrinting().create()

    var fileName: String = ""
    var dpPerPixels: Double = 1.0
    private val bookmarksChangedAction = { serialize() }

    init {
        bookmarks.listeners.add(bookmarksChangedAction)
    }

    fun serialize() {
        val metaFileName = "${fileName}_meta.json"

        val bookmarksList = mutableListOf<BookmarkModel>()
        for (bookmark in bookmarks.items) {
            bookmarksList.add(
                BookmarkModel(
                    bookmark.node.name,
                    bookmark.description,
                    colorToHex(bookmark.color)
                )
            )
        }
        val meta = MetaModel(fileName, dpPerPixels, bookmarksList)
        GlobalScope.launch(Dispatchers.IO) {
            saveToFile(metaFileName, meta)
        }
    }

    private fun saveToFile(metaFileName: String, meta: MetaModel) {
        val currentDir = File(META_DIR)
        if (!currentDir.exists()) {
            currentDir.mkdirs()
        }
        val saveFileName = File(currentDir, metaFileName)

        var outputStream: FileOutputStream? = null
        try {
            outputStream = FileOutputStream(saveFileName)
            val bufferedWriter = BufferedWriter(OutputStreamWriter(outputStream, "UTF-8"))

            gson.toJson(meta, bufferedWriter)
            bufferedWriter.close()
        } catch (e: FileNotFoundException) {
            logger.e("$TAG: save bookmarks error", e)
        } catch (e: IOException) {
            logger.e("$TAG: save bookmarks error", e)
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.flush()
                    outputStream.close()
                } catch (e: IOException) {
                }
            }
        }
    }


    /**
     * call from worker thread.
     */
    fun restoreForFile(fn: String, rootNode: ViewNode?) {
        fileName = fn
        val metaType: Type = object : TypeToken<MetaModel>() {}.type
        try {
            val metaFileName = File(META_DIR, "${fileName}_meta.json")

            val fileReader = FileReader(metaFileName)
            val reader = JsonReader(fileReader)
            val loadedMeta: MetaModel = gson.fromJson(reader, metaType)

            val bookmarkList = mutableListOf<BookmarkInfo>()
            val allNodesMap = mutableMapOf<String, ViewNode>()
            readAllNodes(allNodesMap, rootNode)

            for (bookmark in loadedMeta.bookmarks) {
                val foundNode = allNodesMap[bookmark.viewNodeName] ?: continue
                bookmarkList.add(BookmarkInfo(foundNode, bookmark.description, hexToColor(bookmark.color)))
            }
            dpPerPixels = loadedMeta.dpPerPixels

            // dont trigger when set bookmarks itself
            bookmarks.listeners.remove(bookmarksChangedAction)
            bookmarks.set(bookmarkList)
            bookmarks.listeners.add(bookmarksChangedAction)

        } catch (e: FileNotFoundException) {
            logger.d("$TAG: there is no bookmarks file.")
        } catch (e: Exception) {
            logger.e("$TAG: Cant load bookmarks", e)
        }
    }

    private fun readAllNodes(map: MutableMap<String, ViewNode>, parent: ViewNode?) {
        if (parent == null) {
            return
        }

        map[parent.name] = parent
        for (child in parent.children) {
            readAllNodes(map, child)
        }
    }

    internal data class MetaModel(
        val liFileName: String,
        val dpPerPixels: Double,
        val bookmarks: List<BookmarkModel>
    )

    internal data class BookmarkModel(
        val viewNodeName: String,
        val description: String?,
        val color: String?
    )
}