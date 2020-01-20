package com.github.michaelbrunn3r.maschinenanalyse

import kotlin.math.*

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

fun FloatArray.applyWindow(window_fn: (Int, Int) -> Float ) {
    val N = size
    for(n in indices) {
        set(n, get(n) * window_fn(N, n))
    }
}

val hannWindow: (Int,Int) -> Float = { N: Int, n: Int ->
    (0.5 * (1 - cos((2*Math.PI*n)/N))).toFloat()
    //return sin(Math.PI * n / N).pow(2.0).toFloat()
}