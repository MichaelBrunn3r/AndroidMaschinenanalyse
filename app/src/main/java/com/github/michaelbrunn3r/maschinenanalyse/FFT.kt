package com.github.michaelbrunn3r.maschinenanalyse

import kotlin.math.pow
import kotlin.math.sqrt

fun calcFFTMagnitudes(fft:FloatArray):FloatArray {
    val magnitudes = FloatArray(fft.size)
    for (i in 0 until fft.size/2) {
        magnitudes[i] = complexAbs(fft[i*2],fft[i*2+1])/magnitudes.size // Magnitudes scaled to sample size
    }
    return magnitudes
}

fun complexAbs(Re:Float, Im:Float):Float {
    return sqrt(Re.pow(2)+Im.pow(2))
}

fun fftFrequenzyBin(index:Int, rate:Int, samples:Int):Float {
    return (index * (rate/samples)).toFloat()
}