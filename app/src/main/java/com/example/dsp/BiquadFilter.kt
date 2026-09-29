package com.example.dsp

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Direct Form I / II Biquad Filter implementation for audio DSP.
 */
class BiquadFilter {
    private var b0 = 1.0
    private var b1 = 0.0
    private var b2 = 0.0
    private var a1 = 0.0
    private var a2 = 0.0

    // Direct Form I state variables
    private var x1 = 0.0
    private var x2 = 0.0
    private var y1 = 0.0
    private var y2 = 0.0

    /**
     * Configures this filter as a Peaking EQ filter.
     * @param sampleRate sample rate in Hz (e.g., 44100, 48000)
     * @param centerFreq center frequency in Hz
     * @param gainDb gain in decibels (-12 to +12)
     * @param q quality factor (default 1.414 for 1-octave band)
     */
    fun setPeakingEq(sampleRate: Double, centerFreq: Double, gainDb: Double, q: Double = 1.414) {
        if (sampleRate <= 0.0 || centerFreq <= 0.0) return

        val clampedGain = gainDb.coerceIn(-24.0, 24.0)
        val a = 10.0.pow(clampedGain / 40.0)
        val w0 = 2.0 * PI * (centerFreq.coerceAtMost(sampleRate * 0.49) / sampleRate)
        val sinW0 = sin(w0)
        val cosW0 = cos(w0)
        val alpha = sinW0 / (2.0 * q.coerceAtLeast(0.1))

        val a0 = 1.0 + alpha / a
        b0 = (1.0 + alpha * a) / a0
        b1 = (-2.0 * cosW0) / a0
        b2 = (1.0 - alpha * a) / a0
        a1 = (-2.0 * cosW0) / a0
        a2 = (1.0 - alpha / a) / a0
    }

    /**
     * Configures this filter as a Low-pass filter (used for crossfeed).
     */
    fun setLowPass(sampleRate: Double, cutoffFreq: Double, q: Double = 0.707) {
        val w0 = 2.0 * PI * (cutoffFreq.coerceAtMost(sampleRate * 0.49) / sampleRate)
        val sinW0 = sin(w0)
        val cosW0 = cos(w0)
        val alpha = sinW0 / (2.0 * q)

        val a0 = 1.0 + alpha
        b0 = ((1.0 - cosW0) / 2.0) / a0
        b1 = (1.0 - cosW0) / a0
        b2 = ((1.0 - cosW0) / 2.0) / a0
        a1 = (-2.0 * cosW0) / a0
        a2 = (1.0 - alpha) / a0
    }

    /**
     * Configures this filter as a High-pass filter (used for harmonic super-resolution extraction).
     */
    fun setHighPass(sampleRate: Double, cutoffFreq: Double, q: Double = 0.707) {
        val w0 = 2.0 * PI * (cutoffFreq.coerceAtMost(sampleRate * 0.49) / sampleRate)
        val sinW0 = sin(w0)
        val cosW0 = cos(w0)
        val alpha = sinW0 / (2.0 * q)

        val a0 = 1.0 + alpha
        b0 = ((1.0 + cosW0) / 2.0) / a0
        b1 = (-(1.0 + cosW0)) / a0
        b2 = ((1.0 + cosW0) / 2.0) / a0
        a1 = (-2.0 * cosW0) / a0
        a2 = (1.0 - alpha) / a0
    }

    fun process(sample: Double): Double {
        val out = b0 * sample + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
        x2 = x1
        x1 = sample
        y2 = y1
        y1 = out
        return out
    }

    fun reset() {
        x1 = 0.0
        x2 = 0.0
        y1 = 0.0
        y2 = 0.0
    }

    companion object {
        /**
         * Calculates magnitude response at frequency f for curve rendering in UI.
         */
        fun calculateMagnitudeResponse(
            freq: Double,
            eqGainsDb: FloatArray,
            centerFreqs: FloatArray
        ): Double {
            var totalGainDb = 0.0
            for (i in centerFreqs.indices) {
                val f0 = centerFreqs[i].toDouble()
                val gain = eqGainsDb[i].toDouble()
                // Simple approximate Gaussian bandwidth falloff for visualization
                val octDiff = kotlin.math.abs(kotlin.math.ln(freq / f0) / kotlin.math.ln(2.0))
                val weight = kotlin.math.exp(-0.5 * (octDiff / 0.75).pow(2.0))
                totalGainDb += gain * weight
            }
            return totalGainDb.coerceIn(-18.0, 18.0)
        }
    }
}
