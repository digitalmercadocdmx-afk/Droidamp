package com.example

import com.example.data.EqualizerPresetEntity
import com.example.data.GeminiEqualizerRepository
import com.example.dsp.BiquadFilter
import com.example.dsp.DspParameters
import com.example.dsp.EQ_FREQUENCIES
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testBiquadFilterProcessing() {
        val filter = BiquadFilter()
        filter.setPeakingEq(sampleRate = 44100.0, centerFreq = 1000.0, gainDb = 6.0)

        // Process a impulse signal
        val out1 = filter.process(1.0)
        val out2 = filter.process(0.0)

        // Peaking EQ with positive gain should amplify the impulse
        assertTrue("Filter output should be non-zero", out1 != 0.0)
    }

    @Test
    fun testMagnitudeResponseCalculation() {
        val gains = floatArrayOf(0f, 0f, 0f, 0f, 0f, 6f, 0f, 0f, 0f, 0f)
        val magAt1kHz = BiquadFilter.calculateMagnitudeResponse(1000.0, gains, EQ_FREQUENCIES)
        assertTrue("Magnitude at 1kHz center should be boosted", magAt1kHz > 4.0)

        val magAt31Hz = BiquadFilter.calculateMagnitudeResponse(31.25, gains, EQ_FREQUENCIES)
        assertTrue("Magnitude at 31Hz should remain near 0dB", kotlin.math.abs(magAt31Hz) < 1.0)
    }

    @Test
    fun testPresetEntityCsvSerialization() {
        val gains = listOf(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f)
        val entity = EqualizerPresetEntity.fromGainsList(
            name = "Test Preset",
            genre = "Synthwave",
            gains = gains,
            tubeWarmth = 0.5f,
            superResClarity = 0.8f
        )

        val extracted = entity.toGainsList()
        assertEquals(10, extracted.size)
        assertEquals(1.0f, extracted[0], 0.01f)
        assertEquals(10.0f, extracted[9], 0.01f)
    }

    @Test
    fun testGeminiDeterministicFallbackProfiles() {
        val repo = GeminiEqualizerRepository()
        val synthwaveProfile = repo.getDeterministicProfile("Synthwave retro bass", "Headphones")

        assertEquals("Synthwave", synthwaveProfile.detectedGenre)
        assertEquals(10, synthwaveProfile.eqGainsDb.size)
        assertTrue(synthwaveProfile.superResolutionEnabled)
        assertTrue(synthwaveProfile.tubeWarmth > 0f)

        val metalProfile = repo.getDeterministicProfile("Heavy metal shredding guitars", "Monitors")
        assertEquals("Heavy Metal", metalProfile.detectedGenre)
    }

    @Test
    fun testDspParametersDefault() {
        val params = DspParameters()
        assertEquals(10, params.eqGainsDb.size)
        assertTrue(params.isEqEnabled)
        assertTrue(params.superResolutionEnabled)
        assertTrue(params.isUpscalingEnabled)
        assertEquals(com.example.dsp.ResolutionUpscaleMode.STUDIO_384K, params.upscaleMode)
    }

    @Test
    fun testAudioResolutionMetrics() {
        val metrics = com.example.dsp.AudioResolutionMetrics(
            inputSampleRate = 44100,
            outputSampleRate = 384000,
            outputBitDepth = "32-bit Float Hi-Res",
            isUpscalingActive = true,
            upscaleMultiplier = 384000f / 44100f
        )
        assertTrue(metrics.isUpscalingActive)
        assertEquals("8.71x", metrics.formattedMultiplier)
        assertTrue(metrics.displaySummary.contains("384 kHz"))
        assertTrue(metrics.displaySummary.contains("32-bit Float"))
    }

    @Test
    fun testAudioProcessorUpscaleConfiguration() {
        val processor = com.example.dsp.EqualizerAudioProcessor()
        processor.params = DspParameters(
            isUpscalingEnabled = true,
            upscaleMode = com.example.dsp.ResolutionUpscaleMode.ULTRA_1536K
        )

        val inputFormat = androidx.media3.common.audio.AudioProcessor.AudioFormat(
            44100,
            2,
            androidx.media3.common.C.ENCODING_PCM_16BIT
        )

        val outputFormat = processor.configure(inputFormat)
        // Should upscale 44.1kHz to either 1.536MHz or maximum supported rate (>= 44.1kHz)
        assertTrue("Output sample rate should be >= 44100Hz", outputFormat.sampleRate >= 44100)
        assertEquals(2, outputFormat.channelCount)
    }

    @Test
    fun testAbCompareModeSwitching() {
        val processor = com.example.dsp.EqualizerAudioProcessor()
        processor.abCompareMode = com.example.dsp.AbCompareState.MODE_A_RAW
        assertEquals(com.example.dsp.AbCompareState.MODE_A_RAW, processor.abCompareMode)

        processor.abCompareMode = com.example.dsp.AbCompareState.MODE_B_UPSCALED
        assertEquals(com.example.dsp.AbCompareState.MODE_B_UPSCALED, processor.abCompareMode)
    }

    @Test
    fun testAutoEqGenreDetectionProfiles() {
        val repo = com.example.data.GeminiEqualizerRepository()
        val synthProfile = repo.getDeterministicProfile("Resonance - HOME Synthwave", "Headphones")
        assertEquals("Synthwave", synthProfile.detectedGenre)
        assertEquals(10, synthProfile.eqGainsDb.size)
        // Synthwave should boost sub-bass and high air
        assertTrue("Sub-bass should be boosted for Synthwave", synthProfile.eqGainsDb[0] > 0f)
        assertTrue("Air frequencies should be boosted for Synthwave", synthProfile.eqGainsDb[9] > 0f)

        val rockProfile = repo.getDeterministicProfile("Master of Puppets Heavy Metal", "Monitors")
        assertEquals("Heavy Metal", rockProfile.detectedGenre)
    }

    @Test
    fun testUploadedSongEntity() {
        val song = com.example.data.UploadedSongEntity(
            title = "Nightcall",
            artist = "Kavinsky",
            filePath = "/data/user/0/com.example/files/uploaded_tracks/nightcall.mp3",
            durationMs = 259000L,
            fileSizeFormatted = "4.2 MB",
            mimeType = "audio/mpeg"
        )
        assertEquals("Nightcall", song.title)
        assertEquals("Kavinsky", song.artist)
        assertEquals(259000L, song.durationMs)
        assertEquals("4.2 MB", song.fileSizeFormatted)
    }
}
