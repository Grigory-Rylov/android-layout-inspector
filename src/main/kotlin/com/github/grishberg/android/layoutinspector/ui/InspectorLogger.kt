package com.github.grishberg.android.layoutinspector.ui

import com.github.grishberg.tracerecorder.common.RecorderLogger

class InspectorLogger : RecorderLogger {
    override fun d(msg: String) {
        println(msg)
    }

    override fun e(msg: String) {
        println(msg)
    }

    override fun e(msg: String, t: Throwable) {
        println(msg)
    }

    override fun w(msg: String) {
        println(msg)
    }
}
