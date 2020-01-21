package com.github.michaelbrunn3r.maschinenanalyse

import com.paramsen.noise.Noise
import kotlin.math.*

fun sequenceToFrequencies(samples:FloatArray, window_fn:((Int, Int) -> Float)?, noise:Noise): FloatArray {
    // Apply window function
    if(window_fn != null) {
        val N = samples.size
        for(n in samples.indices) {
            samples[n] *= window_fn(N, n)
        }
    }

    // Apply fft on samples
    val fft_buffer = FloatArray(samples.size+2)
    noise.fft(samples, fft_buffer)

    // Convert fft result (complex numbers) to magnitudes
    return calcFFTMagnitudes(fft_buffer)
}

fun calcFFTMagnitudes(fft:FloatArray):FloatArray {
    val magnitudes = FloatArray(fft.size/2)
    for (i in 0 until fft.size/2) {
        magnitudes[i] = vec2DLength(fft[i*2],fft[i*2+1])/magnitudes.size // Magnitudes scaled to sample size
    }
    return magnitudes
}

fun vec2DLength(Re:Float, Im:Float):Float {
    return sqrt(Re.pow(2)+Im.pow(2))
}

val hannWindow: (Int,Int) -> Float = { N: Int, n: Int ->
    (0.5 * (1 - cos((2*Math.PI*n)/N))).toFloat()
}