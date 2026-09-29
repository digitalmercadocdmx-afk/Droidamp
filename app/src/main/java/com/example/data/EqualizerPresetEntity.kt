package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "equalizer_presets")
@Serializable
data class EqualizerPresetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val genre: String,
    val gainsCsv: String, // 10 comma-separated float strings, e.g. "0.0,2.5,4.0,..."
    val tubeWarmth: Float = 0.35f,
    val superResClarity: Float = 0.65f,
    val crossfeedEnabled: Boolean = false,
    val crossfeedLevel: Float = 0.40f,
    val isFactoryDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toGainsList(): List<Float> {
        return try {
            gainsCsv.split(",").map { it.trim().toFloat() }.let { list ->
                if (list.size == 10) list else List(10) { 0.0f }
            }
        } catch (e: Exception) {
            List(10) { 0.0f }
        }
    }

    companion object {
        fun fromGainsList(
            name: String,
            genre: String,
            gains: List<Float>,
            tubeWarmth: Float = 0.35f,
            superResClarity: Float = 0.65f,
            crossfeedEnabled: Boolean = false,
            crossfeedLevel: Float = 0.40f,
            isFactoryDefault: Boolean = false
        ): EqualizerPresetEntity {
            val csv = gains.joinToString(",") { "%.2f".format(it) }
            return EqualizerPresetEntity(
                name = name,
                genre = genre,
                gainsCsv = csv,
                tubeWarmth = tubeWarmth,
                superResClarity = superResClarity,
                crossfeedEnabled = crossfeedEnabled,
                crossfeedLevel = crossfeedLevel,
                isFactoryDefault = isFactoryDefault
            )
        }
    }
}
