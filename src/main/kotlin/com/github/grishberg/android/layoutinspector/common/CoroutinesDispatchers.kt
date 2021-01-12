package com.github.grishberg.android.layoutinspector.common

import kotlinx.coroutines.CoroutineDispatcher

interface CoroutinesDispatchers {
    val worker: CoroutineDispatcher
    val ui: CoroutineDispatcher
}
