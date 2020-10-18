package com.github.grishberg.android.layoutinspector.settings

interface Settings {
    fun getBoolValueOrDefault(name: String, default: Boolean = false): Boolean
    fun getIntValueOrDefault(name: String, default: Int): Int
    fun getStringValueOrDefault(name: String, default: String): String
    fun getStringValue(name: String): String?
    fun getStringList(name: String): List<String>
    fun setBoolValue(name: String, value: Boolean)
    fun setIntValue(name: String, value: Int)
    fun setStringList(name: String, value: List<String>)
    fun setStringValue(name: String, value: String)
    fun save()
}