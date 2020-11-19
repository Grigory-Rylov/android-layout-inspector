package com.github.grishberg.android.layoutinspector.settings

import com.android.layoutinspector.common.AppLogger
import com.google.gson.GsonBuilder
import com.google.gson.stream.JsonReader
import java.io.*

private const val TAG = "JsonSettings"


class JsonSettings (
    private val log: AppLogger,
    private val baseDir: File
) : Settings {
    private val gson = GsonBuilder().enableComplexMapKeySerialization().setPrettyPrinting().create()

    private val settingsMap = mutableMapOf<String, Any>()
    private val settingsFile = File(baseDir,"android-layout-inspector-settings.json")

    init {
        try {
            val fileReader = FileReader(settingsFile)
            val reader = JsonReader(fileReader)
            val map: Map<String, Any>? = gson.fromJson(reader, MutableMap::class.java)
            if (map != null) {
                settingsMap.putAll(map)
            }
        } catch (e: FileNotFoundException) {
            log.d("$TAG: there is no settings file.")
        } catch (e: Exception) {
            log.e("$TAG: read settings error", e)
        }

    }

    override fun getBoolValueOrDefault(name: String, default: Boolean): Boolean {
        val boolVal = settingsMap[name]
        if (boolVal is Boolean) {
            return boolVal
        }
        return default
    }

    override fun getIntValueOrDefault(name: String, default: Int): Int {
        val value = settingsMap[name]
        if (value is Double) {
            return value.toInt()
        }
        if (value is Int) {
            return value
        }
        return default
    }

    @Suppress("UNCHECKED_CAST")
    override fun getStringList(name: String): List<String> {
        val value = settingsMap[name]
        if (value is List<*>) {
            return value as List<String>
        }
        return listOf()
    }

    override fun setBoolValue(name: String, value: Boolean) {
        settingsMap[name] = value
    }

    override fun setIntValue(name: String, value: Int) {
        settingsMap[name] = value
    }

    override fun setStringValue(name: String, value: String) {
        settingsMap[name] = value
    }

    override fun getStringValueOrDefault(name: String, default: String): String {
        val value = settingsMap[name]
        if (value is String) {
            return value
        }
        return default
    }

    override fun getStringValue(name: String): String? {
        val value = settingsMap[name]
        if (value is String) {
            return value
        }
        return null
    }

    override fun setStringList(name: String, value: List<String>) {
        settingsMap[name] = value
    }

    override fun save() {
        settingsFile.createNewFile()
        var outputStream: FileOutputStream? = null
        try {
            outputStream = FileOutputStream(settingsFile)
            val bufferedWriter: BufferedWriter
            bufferedWriter = BufferedWriter(OutputStreamWriter(outputStream, "UTF-8"))
            gson.toJson(settingsMap, bufferedWriter)
            bufferedWriter.close()
            log.d("$TAG settings are saved")
        } catch (e: FileNotFoundException) {
            log.e("$TAG: save settings error", e)
        } catch (e: IOException) {
            log.e("$TAG: save settings error", e)
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
}