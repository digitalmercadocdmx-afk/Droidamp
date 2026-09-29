package com.example.dsp

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.serialization.Serializable

@Serializable
enum class ResolutionUpscaleMode(
    val id: String,
    val title: String,
    val targetSampleRate: Int,
    val badgeLabel: String,
    val bitDepthLabel: String,
    val description: String
) {
    ULTRA_1536K(
        id = "ultra_1536k",
        title = "Ultra Resolution (1.536 MHz / 32-bit Float)",
        targetSampleRate = 1536000,
        badgeLabel = "1.536 MHz ULTRA (32-BIT)",
        bitDepthLabel = "32-bit Float (IEEE 754)",
        description = "Highest possible 32-bit float resolution with 32x / 34.8x ultra-sampling (1.536 MHz) and 768 kHz ultrasonic Nyquist ceiling"
    ),
    DXD_768K(
        id = "dxd_768k",
        title = "DXD Master (768 kHz / 32-bit Float)",
        targetSampleRate = 768000,
        badgeLabel = "768 kHz DXD (32-BIT)",
        bitDepthLabel = "32-bit Float (IEEE 754)",
        description = "Digital eXtreme Definition 16x oversampling with 384 kHz ultrasonic bandwidth"
    ),
    STUDIO_384K(
        id = "studio_384k",
        title = "Mastering 384 kHz / 32-bit",
        targetSampleRate = 384000,
        badgeLabel = "384 kHz (32-BIT)",
        bitDepthLabel = "32-bit Float (IEEE 754)",
        description = "Studio Reference 8x oversampling with 32-bit floating point precision"
    ),
    AUTO_MAX(
        id = "auto_max",
        title = "High Resolution (192 kHz / 32-bit)",
        targetSampleRate = 192000,
        badgeLabel = "192 kHz MAX",
        bitDepthLabel = "32-bit Float",
        description = "Upscales to 192 kHz with 32-bit float internal headroom"
    ),
    STUDIO_96K(
        id = "studio_96k",
        title = "Studio Master (96 kHz / 32-bit)",
        targetSampleRate = 96000,
        badgeLabel = "96 kHz STUDIO",
        bitDepthLabel = "32-bit Float",
        description = "Audiophile 2x oversampling with anti-aliasing"
    ),
    DIRECT(
        id = "direct",
        title = "Bit-Perfect Direct (1:1)",
        targetSampleRate = 0,
        badgeLabel = "1:1 DIRECT",
        bitDepthLabel = "16-bit Native",
        description = "Bypasses sample rate upscaling, keeping native stream format"
    )
}

@Serializable
enum class AbCompareState(
    val id: String,
    val title: String,
    val badge: String,
    val description: String
) {
    MODE_A_RAW(
        id = "raw_a",
        title = "MODE [A] RAW DIRECT",
        badge = "RAW 1:1",
        description = "Unprocessed native audio stream: 16-bit / standard sample rate, no upscaling, no harmonic excitation"
    ),
    MODE_B_UPSCALED(
        id = "upscaled_b",
        title = "MODE [B] 384 kHz MASTER",
        badge = "384 kHz 32-BIT",
        description = "Studio Reference 384 kHz 32-bit Float upscaled audio with ultrasonic air and dynamic restoration"
    )
}

data class AudioResolutionMetrics(
    val inputSampleRate: Int = 44100,
    val inputBitDepth: String = "16-bit PCM",
    val outputSampleRate: Int = 384000,
    val outputBitDepth: String = "32-bit Float Hi-Res",
    val dspSampleRate: Int = 384000,
    val isUpscalingActive: Boolean = true,
    val upscaleMultiplier: Float = 8.71f,
    val mode: ResolutionUpscaleMode = ResolutionUpscaleMode.STUDIO_384K,
    val abCompareState: AbCompareState = AbCompareState.MODE_B_UPSCALED,
    val isAutoCycleActive: Boolean = false,
    val autoCycleSecondsRemaining: Int = 0,
    val ultrasonicEnergy: Float = 0.65f, // 0.0 to 1.0 live energy above 16kHz
    val maxHardwareSupportedRate: Int = 192000
) {
    val formattedMultiplier: String
        get() = if (isUpscalingActive && upscaleMultiplier > 1.0f) "%.2fx".format(upscaleMultiplier) else "1.00x"

    val displaySummary: String
        get() = if (isUpscalingActive) {
            val outStr = if (outputSampleRate >= 1000000) "%.3f MHz".format(outputSampleRate / 1000000.0) else "${outputSampleRate / 1000} kHz"
            val inStr = "${inputSampleRate / 1000.0} kHz"
            "$inStr ➔ $outStr • $outputBitDepth ($formattedMultiplier)"
        } else {
            "${inputSampleRate / 1000.0} kHz Native Direct"
        }
}

object AudioResolutionCapabilities {
    private const val TAG = "AudioResCaps"
    private var cachedMaxRate: Int? = null

    /**
     * Determines the supported physical AudioTrack sample rate safely without allocating native tracks.
     */
    fun getMaximumSupportedSampleRate(context: Context? = null, preferredTarget: Int = 192000): Int {
        cachedMaxRate?.let { return it.coerceAtMost(preferredTarget) }

        val nativeRate = try {
            if (context != null) {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager
                audioManager?.getProperty(android.media.AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)?.toIntOrNull()
            } else null
        } catch (ignored: Exception) {
            null
        } ?: 48000

        cachedMaxRate = nativeRate
        return nativeRate.coerceAtMost(preferredTarget)
    }

    fun isEncodingSupported(encoding: Int, sampleRate: Int = 48000): Boolean {
        return isSampleRateSupported(sampleRate, encoding)
    }

    /**
     * Safe verification using getMinBufferSize without hardware allocation crashes.
     */
    fun isSampleRateSupported(rate: Int, encoding: Int = AudioFormat.ENCODING_PCM_16BIT): Boolean {
        if (rate < 4000 || rate > 192000) return false
        return try {
            val minBuf = AudioTrack.getMinBufferSize(
                rate,
                AudioFormat.CHANNEL_OUT_STEREO,
                encoding
            )
            minBuf > 0
        } catch (t: Throwable) {
            false
        }
    }
}
