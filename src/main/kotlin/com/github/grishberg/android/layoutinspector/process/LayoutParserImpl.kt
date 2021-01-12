package com.github.grishberg.android.layoutinspector.process

import com.android.layoutinspector.parser.LayoutFileDataParser
import com.github.grishberg.android.layoutinspector.domain.LayoutParserInput
import java.io.File

class LayoutParserImpl : LayoutParserInput {
    override fun parseFromBytes(bytes: ByteArray) = LayoutFileDataParser.parseFromBytes(bytes)


    override fun parseFromFile(file: File) = LayoutFileDataParser.parseFromFile(file)
}
