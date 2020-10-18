package com.github.grishberg.android.layoutinspector.domain

interface RecordInfoInput {
    suspend fun getLayoutRecordOptions(): LayoutRecordOptions?
}