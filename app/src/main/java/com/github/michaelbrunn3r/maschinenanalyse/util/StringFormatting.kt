package com.github.michaelbrunn3r.maschinenanalyse.util

import java.util.concurrent.TimeUnit

fun formatTime(time: Long, unit: TimeUnit): String {
    var t = time

    val parts = ArrayList<String>()
    for(tu in TimeUnit.values().reversed()) {
        val value = unit.convertTo(t, tu)
        if(value > 0) {
            parts.add("${value}${tu.symbol()}")
            t -= tu.toMillis(value)
        }
    }

    return parts.joinToString(" ")
}

fun TimeUnit.convertTo(value: Long, to: TimeUnit): Long {
    return when(to) {
        TimeUnit.DAYS -> toDays(value)
        TimeUnit.HOURS -> toHours(value)
        TimeUnit.MINUTES -> toMinutes(value)
        TimeUnit.SECONDS -> toSeconds(value)
        TimeUnit.MILLISECONDS -> toMillis(value)
        TimeUnit.MICROSECONDS -> toMicros(value)
        TimeUnit.NANOSECONDS -> toNanos(value)
    }
}

fun TimeUnit.symbol(): String {
    return when(this) {
        TimeUnit.DAYS -> "d"
        TimeUnit.HOURS -> "h"
        TimeUnit.MINUTES -> "m"
        TimeUnit.SECONDS -> "s"
        TimeUnit.MILLISECONDS -> "ms"
        TimeUnit.MICROSECONDS -> "us"
        TimeUnit.NANOSECONDS -> "ns"
    }
}