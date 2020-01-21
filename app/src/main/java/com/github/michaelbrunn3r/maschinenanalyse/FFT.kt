package com.github.michaelbrunn3r.maschinenanalyse

import com.paramsen.noise.Noise
import kotlin.math.*

class FFT {
    companion object {

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

        fun calcFFTMagnitudes(fft_imaginary:FloatArray):FloatArray {
            val magnitudes = FloatArray(fft_imaginary.size/2)
            for (i in 0 until fft_imaginary.size/2) {
                magnitudes[i] = vec2DLength(fft_imaginary[i*2],fft_imaginary[i*2+1])/magnitudes.size // Magnitudes scaled to sample size
            }
            return magnitudes
        }

        /**
         * Calculates the length of a 2D vector
         */
        fun vec2DLength(Re:Float, Im:Float):Float {
            return sqrt(Re.pow(2)+Im.pow(2))
        }

        /**
         * Returns the number of frequencies that can be calculated with a given number of Samples
         */
        fun numFrequenciesFor(numSamples:Int): Int {
            return (numSamples+2)/2
        }

        fun nyquist(frequency:Float): Float {
            return frequency/2
        }

        val hannWindow: (Int,Int) -> Float = { N: Int, n: Int ->
            (0.5 * (1 - cos((2*Math.PI*n)/N))).toFloat()
        }
    }
}