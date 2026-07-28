package com.almica.ramani.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Date.simpleString(): String = toSimpleString(this)

fun toSimpleString(date: Date?): String = with(date ?: Date()) {
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(this)
}

fun Date.simpleStringWithTime(): String = toSimpleStringWithTime(this)

fun toSimpleStringWithTime(date: Date?): String = with(date ?: Date()) {
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(this)
}

fun Date.simpleStringTime(): String = toSimpleStringTime(this)

fun toSimpleStringTime(date: Date?): String = with(date ?: Date()) {
    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(this)
}

fun Date.simpleStringDeltaTime(): String = toSimpleStringDeltaTime(this)

fun toSimpleStringDeltaTime(date: Date?): String = with(date ?: Date()) {
    SimpleDateFormat("HH:mm:ss", Locale.GERMANY).format(this)
}
