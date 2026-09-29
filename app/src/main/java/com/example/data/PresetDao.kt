package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PresetDao {
    @Query("SELECT * FROM equalizer_presets ORDER BY isFactoryDefault DESC, name ASC")
    fun getAllPresets(): Flow<List<EqualizerPresetEntity>>

    @Query("SELECT * FROM equalizer_presets WHERE id = :id LIMIT 1")
    suspend fun getPresetById(id: Long): EqualizerPresetEntity?

    @Query("SELECT COUNT(*) FROM equalizer_presets")
    suspend fun getPresetCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: EqualizerPresetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(presets: List<EqualizerPresetEntity>)

    @Update
    suspend fun updatePreset(preset: EqualizerPresetEntity)

    @Delete
    suspend fun deletePreset(preset: EqualizerPresetEntity)

    @Query("DELETE FROM equalizer_presets WHERE id = :id AND isFactoryDefault = 0")
    suspend fun deleteUserPreset(id: Long)
}
