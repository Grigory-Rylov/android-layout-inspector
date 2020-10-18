/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.layoutinspector
import com.google.gson.JsonObject
import com.google.gson.JsonParser
enum class ProtocolVersion(val value: String) {
    Version1("1"),
    Version2("2")
}
class LayoutInspectorCaptureOptions {
    var version = ProtocolVersion.Version1
    var title = ""
    override fun toString(): String {
        return serialize()
    }
    private fun serialize(): String {
        val obj = JsonObject()
        obj.addProperty(VERSION, version.value)
        obj.addProperty(TITLE, title)
        return obj.toString()
    }
    fun parse(json: String) {
        val obj = JsonParser().parse(json).asJsonObject
        version = ProtocolVersion.valueOf("Version${obj.get(VERSION).asString}")
        title = obj.get(TITLE).asString
    }
    companion object {
        private val VERSION = "version"
        private val TITLE = "title"
    }
}