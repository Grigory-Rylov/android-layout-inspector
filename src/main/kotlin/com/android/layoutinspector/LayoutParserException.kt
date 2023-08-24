package com.android.layoutinspector

open class LayoutParserException : Exception {
    constructor() : super()

    constructor(message: String) : super(message)

    constructor(e: Throwable) : super(e)

    constructor(message: String, t: Throwable) : super(message)
}
