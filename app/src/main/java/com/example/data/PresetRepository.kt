package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class PresetRepository(private val presetDao: PresetDao) {

    val allPresets: Flow<List<EqualizerPresetEntity>> = presetDao.getAllPresets()

    suspend fun savePreset(preset: EqualizerPresetEntity): Long = withContext(Dispatchers.IO) {
        presetDao.insertPreset(preset)
    }

    suspend fun deleteUserPreset(id: Long) = withContext(Dispatchers.IO) {
        presetDao.deleteUserPreset(id)
    }

    suspend fun ensureDefaultPresets() = withContext(Dispatchers.IO) {
        if (presetDao.getPresetCount() == 0) {
            presetDao.insertAll(AppDatabase.FACTORY_PRESETS)
        }
    }
}
