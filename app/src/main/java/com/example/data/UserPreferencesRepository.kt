package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.dsp.ResolutionUpscaleMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "droidamp_settings")

enum class CyberdeckSkin(val id: String, val title: String) {
    MATRIX_GREEN("matrix_green", "Cyberdeck Green (Matrix)"),
    SYNTHWAVE_AMBER("synthwave_amber", "OLED Amber (Synthwave)"),
    NEON_CYBERPUNK("neon_cyberpunk", "Chroma Neon (Cyberpunk)")
}

data class UserSettings(
    val skin: CyberdeckSkin = CyberdeckSkin.MATRIX_GREEN,
    val lastStationUrl: String = "",
    val lastStationName: String = "",
    val cue1Ms: Long = 0L,
    val cue2Ms: Long = 0L,
    val cue3Ms: Long = 0L,
    val cue4Ms: Long = 0L,
    val playbackPitch: Float = 1.0f,
    val upscaleMode: ResolutionUpscaleMode = ResolutionUpscaleMode.STUDIO_384K,
    val isUpscalingEnabled: Boolean = true,
    val ultrasonicAir: Float = 0.70f,
    val transientSharpening: Float = 0.50f
)

class UserPreferencesRepository(private val context: Context) {
    private val keySkin = stringPreferencesKey("app_skin")
    private val keyLastUrl = stringPreferencesKey("last_station_url")
    private val keyLastName = stringPreferencesKey("last_station_name")
    private val keyCue1 = stringPreferencesKey("cue_1")
    private val keyCue2 = stringPreferencesKey("cue_2")
    private val keyCue3 = stringPreferencesKey("cue_3")
    private val keyCue4 = stringPreferencesKey("cue_4")
    private val keyPitch = floatPreferencesKey("playback_pitch")
    private val keyUpscaleMode = stringPreferencesKey("upscale_mode")
    private val keyUpscaleEnabled = androidx.datastore.preferences.core.booleanPreferencesKey("upscale_enabled")
    private val keyUltrasonicAir = floatPreferencesKey("ultrasonic_air")
    private val keyTransientSharpening = floatPreferencesKey("transient_sharpening")

    val userSettingsFlow: Flow<UserSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
        val skinId = prefs[keySkin] ?: CyberdeckSkin.MATRIX_GREEN.id
        val skin = CyberdeckSkin.entries.find { it.id == skinId } ?: CyberdeckSkin.MATRIX_GREEN
        val upscaleModeId = prefs[keyUpscaleMode] ?: ResolutionUpscaleMode.STUDIO_384K.id
        val upscaleMode = ResolutionUpscaleMode.entries.find { it.id == upscaleModeId } ?: ResolutionUpscaleMode.STUDIO_384K

        UserSettings(
            skin = skin,
            lastStationUrl = prefs[keyLastUrl] ?: "",
            lastStationName = prefs[keyLastName] ?: "",
            cue1Ms = prefs[keyCue1]?.toLongOrNull() ?: 0L,
            cue2Ms = prefs[keyCue2]?.toLongOrNull() ?: 0L,
            cue3Ms = prefs[keyCue3]?.toLongOrNull() ?: 0L,
            cue4Ms = prefs[keyCue4]?.toLongOrNull() ?: 0L,
            playbackPitch = prefs[keyPitch] ?: 1.0f,
            upscaleMode = upscaleMode,
            isUpscalingEnabled = prefs[keyUpscaleEnabled] ?: true,
            ultrasonicAir = prefs[keyUltrasonicAir] ?: 0.70f,
            transientSharpening = prefs[keyTransientSharpening] ?: 0.50f
        )
    }

    suspend fun setSkin(skin: CyberdeckSkin) {
        context.dataStore.edit { prefs ->
            prefs[keySkin] = skin.id
        }
    }

    suspend fun setLastStation(name: String, url: String) {
        context.dataStore.edit { prefs ->
            prefs[keyLastName] = name
            prefs[keyLastUrl] = url
        }
    }

    suspend fun setHotCue(cueIndex: Int, positionMs: Long) {
        context.dataStore.edit { prefs ->
            when (cueIndex) {
                1 -> prefs[keyCue1] = positionMs.toString()
                2 -> prefs[keyCue2] = positionMs.toString()
                3 -> prefs[keyCue3] = positionMs.toString()
                4 -> prefs[keyCue4] = positionMs.toString()
            }
        }
    }

    suspend fun setPlaybackPitch(pitch: Float) {
        context.dataStore.edit { prefs ->
            prefs[keyPitch] = pitch
        }
    }

    suspend fun setUpscaleMode(mode: ResolutionUpscaleMode) {
        context.dataStore.edit { prefs ->
            prefs[keyUpscaleMode] = mode.id
        }
    }

    suspend fun setUpscalingEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[keyUpscaleEnabled] = enabled
        }
    }

    suspend fun setUltrasonicAir(air: Float) {
        context.dataStore.edit { prefs ->
            prefs[keyUltrasonicAir] = air
        }
    }

    suspend fun setTransientSharpening(sharpening: Float) {
        context.dataStore.edit { prefs ->
            prefs[keyTransientSharpening] = sharpening
        }
    }
}
