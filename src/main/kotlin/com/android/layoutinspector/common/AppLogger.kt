package com.android.layoutinspector.common

interface AppLogger {
    fun d(msg: String)
    fun e(msg: String)
    fun e(msg: String, t: Throwable)
    fun w(msg: String)
}