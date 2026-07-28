package com.almica.ramani.utils

import android.util.Log

class MyLog(var enabled: Boolean, val TAG: String) {

    fun isEnabled(): Boolean {
        return enabled
    }

    fun i(msg: String) {
        if (enabled) {
            val t = Throwable()
            val elements = t.stackTrace

            val callerClassName = elements[1].className
            val callerMethodName = elements[1].methodName
            val callerLineNumber = elements[1].lineNumber

            Log.i(TAG, "$callerClassName[$callerMethodName:$callerLineNumber] $msg")
            //			Log.i(TAG, msg);
        }
    }

    fun i() {
        if (enabled) {
            val t = Throwable()
            val elements = t.stackTrace

            val callerClassName = elements[1].className
            val callerMethodName = elements[1].methodName
            val callerLineNumber = elements[1].lineNumber

            Log.i(TAG, "$callerClassName[$callerMethodName:$callerLineNumber]")
            //			Log.i(TAG, msg);
        }
    }

    /**
     * ignore enable disable
     * @param msg
     */
    fun I(msg: String) {
        val t = Throwable()
        val elements = t.stackTrace

        val callerClassName = elements[1].className
        val callerMethodName = elements[1].methodName
        val callerLineNumber = elements[1].lineNumber

        Log.i(TAG, "$callerClassName[$callerMethodName:$callerLineNumber] $msg")
    }

    fun I() {
        val t = Throwable()
        val elements = t.stackTrace

        val callerClassName = elements[1].className
        val callerMethodName = elements[1].methodName
        val callerLineNumber = elements[1].lineNumber

        Log.i(TAG, "$callerClassName[$callerMethodName:$callerLineNumber]")
    }

    fun ii(msg: String) {
        if (enabled) {
            val t = Throwable()
            val elements = t.stackTrace
            Log.i(TAG, "================================================")
            for (n in elements.indices) {
                val callerClassName = elements[n].className
                val callerMethodName = elements[n].methodName
                val callerLineNumber = elements[1].lineNumber
                Log.i(TAG, "$callerClassName[$callerMethodName:$callerLineNumber] $msg")
            }
        }
    }

    fun enable() {
        enabled = true
    }

    fun disable() {
        enabled = false
    }

}