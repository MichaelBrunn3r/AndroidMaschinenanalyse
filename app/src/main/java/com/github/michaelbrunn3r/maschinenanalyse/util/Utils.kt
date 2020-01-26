package com.github.michaelbrunn3r.maschinenanalyse.util

import android.nfc.FormatException
fun isBitFlagSet(value:Int, flag:Int):Boolean {return value and flag != 0}

fun String.parseRange(): Pair<Int?,Int?> {
    val values = split("..").map { it.toIntOrNull() }
    if(values.size != 2) throw FormatException("Range has to be of format: '[Int]-[Int]'")
    return Pair(values[0],values[1])
}