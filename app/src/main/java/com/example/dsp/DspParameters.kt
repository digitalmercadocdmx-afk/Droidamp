package com.example.dsp

import kotlinx.serialization.Serializable

/**
 * 10 Equalizer Center Frequencies in Hz:
 * 31Hz, 62Hz, 125Hz, 250Hz, 500Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz
 */
val EQ_FREQUENCIES = floatArrayOf(
    31.25f, 62.5f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f
)

val EQ_FREQ_LABELS = listOf(
    "31Hz", "62Hz", "125Hz", "250Hz", "500Hz", "1kHz", "2kHz", "4kHz", "8kHz", "16kHz"
)

@Serializable
data class DspParameters(
    val eqGainsDb: List<Float> = List(10) { 0.0f }, // Range: -12dB to +12dB
    val isEqEnabled: Boolean = true,
    val superResolutionEnabled: Boolean = true,
    val superResClarity: Float = 0.65f, // 0.0 to 1.0
    val tubeWarmth: Float = 0.35f, // 0.0 to 1.0
    val crossfeedEnabled: Boolean = false,
    val crossfeedLevel: Float = 0.40f, // 0.0 to 1.0
    val masterGainDb: Float = 0.0f,
    // Output Resolution Upscaling (384 kHz / 32-bit Float)
    val upscaleMode: ResolutionUpscaleMode = ResolutionUpscaleMode.STUDIO_384K,
    val isUpscalingEnabled: Boolean = true,
    val ultrasonicAirLevel: Float = 0.70f, // 0.0 to 1.0
    val transientSharpening: Float = 0.50f // 0.0 to 1.0
)

data class VuMeterState(
    val leftRms: Float = 0f,
    val rightRms: Float = 0f,
    val leftPeak: Float = 0f,
    val rightPeak: Float = 0f
)
