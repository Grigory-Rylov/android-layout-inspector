/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.layoutinspector.parser
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.*
/**
 * Decodes view hierarchy v2 protocol created by ViewHierarchyEncoder in Android framework.
 */
internal class ViewNodeV2Decoder(private val mBuf: ByteBuffer) {
    fun hasRemaining(): Boolean {
        return mBuf.hasRemaining()
    }
    fun readObject(): Any {
        val sig = mBuf.get()
        return when (sig) {
            SIG_BOOLEAN -> if (mBuf.get().toInt() == 0) java.lang.Boolean.FALSE else java.lang.Boolean.TRUE
            SIG_BYTE -> mBuf.get()
            SIG_SHORT -> mBuf.short
            SIG_INT -> mBuf.int
            SIG_LONG -> mBuf.long
            SIG_FLOAT -> mBuf.float
            SIG_DOUBLE -> mBuf.double
            SIG_STRING -> readString()
            SIG_MAP -> readMap()
            else -> throw DecoderException(
                sig,
                mBuf.position() - 1
            )
        }
    }
    private fun readString(): String {
        val len = mBuf.short.toInt()
        val b = ByteArray(len)
        mBuf.get(b, 0, len)
        return String(b, Charset.forName("utf-8"))
    }
    private fun readMap(): Map<Short, Any> {
        val m = HashMap<Short, Any>()
        while (true) {
            val o = readObject()
            if (o !is Short) {
                throw DecoderException("Expected short key, got " + o.javaClass)
            }
            if (o == SIG_END_MAP) {
                break
            }
            m[o] = readObject()
        }
        return m
    }
    class DecoderException : RuntimeException {
        constructor(
            seen: Byte,
            pos: Int
        ) : super(String.format("Unexpected byte %c seen at position %d", seen.toChar(), pos))
        constructor(msg: String) : super(msg)
    }
    companion object {
        // Prefixes for simple primitives. These match the JNI definitions.
        const val SIG_BOOLEAN: Byte = 'Z'.toByte()
        const val SIG_BYTE: Byte = 'B'.toByte()
        const val SIG_SHORT: Byte = 'S'.toByte()
        const val SIG_INT: Byte = 'I'.toByte()
        const val SIG_LONG: Byte = 'J'.toByte()
        const val SIG_FLOAT: Byte = 'F'.toByte()
        const val SIG_DOUBLE: Byte = 'D'.toByte()
        // Prefixes for some commonly used objects
        const val SIG_STRING: Byte = 'R'.toByte()
        const val SIG_MAP: Byte = 'M'.toByte() // a map with an short key
        const val SIG_END_MAP: Short = 0
    }
}