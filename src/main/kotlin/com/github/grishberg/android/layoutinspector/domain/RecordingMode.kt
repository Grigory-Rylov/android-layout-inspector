package com.github.grishberg.android.layoutinspector.domain

sealed interface RecordingMode {
    object Layouts: RecordingMode {

        override fun toString(): String = "Layouts"
    }

    object LayoutsAndComposeDump: RecordingMode {

        override fun toString(): String = "Layouts and compose dump"
    }

    object LayoutsAndDump: RecordingMode {

        override fun toString(): String = "Layouts and dump"
    }

    object Dump : RecordingMode {

        override fun toString(): String = "Hierarchy dump"
    }
}
