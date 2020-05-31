package com.github.grishberg.android.layoutinspector.domain

import java.io.File

interface DialogsInput {
    fun showOpenFileDialogAndReturnResult(): File?
}