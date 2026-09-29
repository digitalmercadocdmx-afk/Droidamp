package com.example.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

class SongUploadManager(private val context: Context) {

    private val tracksDir = File(context.filesDir, "uploaded_tracks").apply {
        if (!exists()) mkdirs()
    }

    suspend fun importAudioFromUri(uri: Uri): Result<UploadedSongEntity> = withContext(Dispatchers.IO) {
        try {
            var displayName = "Uploaded_Track_${System.currentTimeMillis()}"
            var fileSize = 0L

            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        displayName = cursor.getString(nameIndex) ?: displayName
                    }
                    if (sizeIndex != -1) {
                        fileSize = cursor.getLong(sizeIndex)
                    }
                }
            }

            // Copy stream to internal storage
            val cleanName = displayName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val destFile = File(tracksDir, "${System.currentTimeMillis()}_$cleanName")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(Exception("Cannot open stream for $uri"))

            if (fileSize <= 0) {
                fileSize = destFile.length()
            }

            // Extract metadata
            var durationMs = 0L
            var artist = "Local Device"
            var title = displayName.substringBeforeLast(".")

            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(destFile.absolutePath)
                val metaTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                val metaArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                val metaDur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)

                if (!metaTitle.isNullOrBlank()) title = metaTitle
                if (!metaArtist.isNullOrBlank()) artist = metaArtist
                if (!metaDur.isNullOrBlank()) durationMs = metaDur.toLongOrNull() ?: 0L
            } catch (e: Exception) {
                // Keep file-based defaults
            } finally {
                retriever.release()
            }

            val formattedSize = formatFileSize(fileSize)
            val mimeType = context.contentResolver.getType(uri) ?: "audio/mpeg"

            val song = UploadedSongEntity(
                title = title,
                artist = artist,
                filePath = destFile.absolutePath,
                durationMs = durationMs,
                fileSizeFormatted = formattedSize,
                mimeType = mimeType,
                addedTimestamp = System.currentTimeMillis()
            )

            Result.success(song)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generates a local demo synthwave WAV audio file so users can test uploaded local audio immediately
     * even when no audio files are present in the test emulator storage.
     */
    suspend fun generateDemoCyberdeckTrack(): Result<UploadedSongEntity> = withContext(Dispatchers.IO) {
        try {
            val destFile = File(tracksDir, "demo_tensorrt_synthwave_arpeggio.wav")
            if (!destFile.exists()) {
                writeSynthwaveWavFile(destFile, durationSeconds = 16)
            }

            val song = UploadedSongEntity(
                title = "TensorRT Cyberdeck Synthwave",
                artist = "DSP Neural Engine",
                filePath = destFile.absolutePath,
                durationMs = 16000L,
                fileSizeFormatted = formatFileSize(destFile.length()),
                mimeType = "audio/wav",
                addedTimestamp = System.currentTimeMillis()
            )

            Result.success(song)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateLoFiTrack(): Result<UploadedSongEntity> = withContext(Dispatchers.IO) {
        try {
            val destFile = File(tracksDir, "demo_cyberdeck_lofi_chill.wav")
            if (!destFile.exists()) {
                writeSynthwaveWavFile(destFile, durationSeconds = 12)
            }

            val song = UploadedSongEntity(
                title = "Cyberdeck 808 Lo-Fi Beat",
                artist = "Analog Tube Engine",
                filePath = destFile.absolutePath,
                durationMs = 12000L,
                fileSizeFormatted = formatFileSize(destFile.length()),
                mimeType = "audio/wav",
                addedTimestamp = System.currentTimeMillis()
            )

            Result.success(song)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importAudioFromUrl(url: String, customTitle: String? = null): Result<UploadedSongEntity> = withContext(Dispatchers.IO) {
        try {
            val cleanTitle = if (!customTitle.isNullOrBlank()) customTitle.trim() else url.substringAfterLast("/").substringBefore("?").ifBlank { "Stream_Track_${System.currentTimeMillis()}" }
            val cleanName = cleanTitle.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val destFile = File(tracksDir, "${System.currentTimeMillis()}_$cleanName.mp3")

            URL(url).openStream().use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            val retriever = MediaMetadataRetriever()
            var durationMs = 0L
            var artist = "Online Audio"
            var title = cleanTitle

            try {
                retriever.setDataSource(destFile.absolutePath)
                val metaTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                val metaArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                val metaDur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                if (!metaTitle.isNullOrBlank()) title = metaTitle
                if (!metaArtist.isNullOrBlank()) artist = metaArtist
                if (!metaDur.isNullOrBlank()) durationMs = metaDur.toLongOrNull() ?: 0L
            } catch (ignored: Exception) {
            } finally {
                retriever.release()
            }

            val song = UploadedSongEntity(
                title = title,
                artist = artist,
                filePath = destFile.absolutePath,
                durationMs = durationMs,
                fileSizeFormatted = formatFileSize(destFile.length()),
                mimeType = "audio/mpeg",
                addedTimestamp = System.currentTimeMillis()
            )

            Result.success(song)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun deleteSongFile(filePath: String) {
        try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (ignored: Exception) {}
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
            else -> "$bytes B"
        }
    }

    private fun writeSynthwaveWavFile(file: File, durationSeconds: Int) {
        val sampleRate = 44100
        val numSamples = sampleRate * durationSeconds
        val numChannels = 2
        val bytesPerSample = 2
        val dataSize = numSamples * numChannels * bytesPerSample

        FileOutputStream(file).use { out ->
            // WAV Header
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray())
            header.putInt(36 + dataSize)
            header.put("WAVE".toByteArray())
            header.put("fmt ".toByteArray())
            header.putInt(16) // PCM subchunk size
            header.putShort(1) // AudioFormat 1 = PCM
            header.putShort(numChannels.toShort())
            header.putInt(sampleRate)
            header.putInt(sampleRate * numChannels * bytesPerSample) // ByteRate
            header.putShort((numChannels * bytesPerSample).toShort()) // BlockAlign
            header.putShort((bytesPerSample * 8).toShort()) // BitsPerSample
            header.put("data".toByteArray())
            header.putInt(dataSize)

            out.write(header.array())

            // Synthwave arpeggiator chords: Am - F - C - G
            val chordProgression = listOf(
                listOf(220.0, 261.63, 329.63, 440.0), // Am
                listOf(174.61, 220.0, 261.63, 349.23), // F
                listOf(130.81, 164.81, 196.0, 261.63), // C
                listOf(196.0, 246.94, 293.66, 392.0)  // G
            )

            val buffer = ByteBuffer.allocate(4096).order(ByteOrder.LITTLE_ENDIAN)
            val noteDurationSamples = sampleRate / 4 // 16th notes at 120 bpm = 4 notes/sec

            for (i in 0 until numSamples) {
                val chordIndex = ((i / (sampleRate * 4)) % chordProgression.size)
                val currentChord = chordProgression[chordIndex]
                val noteIndex = ((i / (noteDurationSamples / 2)) % currentChord.size)
                val freq = currentChord[noteIndex]

                val t = i.toDouble() / sampleRate
                // Bass drone
                val bassFreq = currentChord[0] / 2.0
                val bass = sin(2.0 * PI * bassFreq * t) * 0.35

                // Lead sawtooth emulation
                val phase = (freq * t) % 1.0
                val saw = (2.0 * phase - 1.0) * 0.25

                // Kick drum on beat (every 0.5s)
                val beatPos = t % 0.5
                val kick = if (beatPos < 0.12) {
                    val kickFreq = 120.0 * (1.0 - beatPos / 0.12) + 40.0
                    sin(2.0 * PI * kickFreq * beatPos) * (1.0 - beatPos / 0.12) * 0.45
                } else 0.0

                var sampleL = (bass + saw * 0.8 + kick).coerceIn(-1.0, 1.0)
                var sampleR = (bass + saw * 1.2 + kick).coerceIn(-1.0, 1.0)

                val shortL = (sampleL * 30000.0).toInt().toShort()
                val shortR = (sampleR * 30000.0).toInt().toShort()

                if (buffer.remaining() < 4) {
                    out.write(buffer.array(), 0, buffer.position())
                    buffer.clear()
                }

                buffer.putShort(shortL)
                buffer.putShort(shortR)
            }

            if (buffer.position() > 0) {
                out.write(buffer.array(), 0, buffer.position())
            }
        }
    }
}
