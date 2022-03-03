package com.kkstream.imademo.extend

import java.util.*

fun Long.getStringForTime(): String {
    val timeUnset = java.lang.Long.MIN_VALUE + 1
    val formatBuilder = StringBuilder()
    val formatter = Formatter(formatBuilder, Locale.getDefault())

    val timeMs = if (this == timeUnset) 0 else this
    val totalSeconds = timeMs / 1000

    val seconds = totalSeconds % 60
    val minutes = totalSeconds / 60 % 60
    val hours = totalSeconds / 3600

    formatBuilder.setLength(0)
    return if (hours > 0)
        formatter.format("%02d:%02d:%02d", hours, minutes, seconds).toString()
    else
        formatter.format("%02d:%02d", minutes, seconds).toString()
}

fun Long.toSecond(): Double {
    return (this.div(1000L)).toDouble()
}

fun Double.toMilliSecond(): Long {
    return (this.times(1000L)).toLong()
}