package com.github.grishberg.android.layoutinspector.common

import com.intellij.openapi.application.EDT
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class CoroutinesDispatchersImpl : CoroutinesDispatchers {
    override val worker: CoroutineDispatcher = Dispatchers.IO
    override val ui: CoroutineDispatcher = Dispatchers.EDT as CoroutineDispatcher
}
