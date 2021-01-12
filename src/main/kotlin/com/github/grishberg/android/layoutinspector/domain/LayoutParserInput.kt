package com.github.grishberg.android.layoutinspector.domain

import com.android.layoutinspector.model.LayoutFileData
import java.io.File

interface LayoutParserInput {
    fun parseFromBytes(bytes: ByteArray): LayoutFileData

    fun parseFromFile(file: File): LayoutFileData
}
