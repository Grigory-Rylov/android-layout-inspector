package com.github.grishberg.android.layoutinspector.domain

interface LayoutRecordOptionsInput {
    suspend fun getLayoutOptions(): LayoutRecordOptions?
}
