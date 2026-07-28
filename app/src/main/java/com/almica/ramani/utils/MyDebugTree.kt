package com.almica.ramani.utils

import timber.log.Timber.DebugTree


class MyDebugTree : DebugTree() {
    override fun createStackElementTag(element: StackTraceElement): String {
        return String.format(
            "[L:%s] [M:%s] [C:%s]",
            element.lineNumber,
            element.methodName,
            super.createStackElementTag(element)
        )
    }
}