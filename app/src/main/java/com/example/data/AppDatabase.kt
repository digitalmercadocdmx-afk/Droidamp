package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [EqualizerPresetEntity::class, UploadedSongEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun presetDao(): PresetDao
    abstract fun uploadedSongDao(): UploadedSongDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "droidamp_dsp_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                getDatabase(context).presetDao().insertAll(FACTORY_PRESETS)
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        val FACTORY_PRESETS = listOf(
            EqualizerPresetEntity.fromGainsList(
                name = "Flat Line",
                genre = "Reference",
                gains = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
                tubeWarmth = 0.0f,
                superResClarity = 0.5f,
                crossfeedEnabled = false,
                isFactoryDefault = true
            ),
            EqualizerPresetEntity.fromGainsList(
                name = "Synthwave 1984",
                genre = "Synthwave",
                gains = listOf(5.5f, 4.0f, 2.0f, -1.0f, -0.5f, 1.0f, 2.5f, 4.0f, 5.0f, 4.5f),
                tubeWarmth = 0.55f,
                superResClarity = 0.75f,
                crossfeedEnabled = true,
                crossfeedLevel = 0.45f,
                isFactoryDefault = true
            ),
            EqualizerPresetEntity.fromGainsList(
                name = "Cyberdeck Club",
                genre = "Electronic",
                gains = listOf(6.0f, 5.0f, 2.5f, -1.5f, -2.0f, 0.5f, 2.0f, 3.5f, 4.5f, 5.5f),
                tubeWarmth = 0.40f,
                superResClarity = 0.80f,
                crossfeedEnabled = false,
                isFactoryDefault = true
            ),
            EqualizerPresetEntity.fromGainsList(
                name = "TensorRT Neural HD",
                genre = "Super Resolution",
                gains = listOf(1.5f, 1.0f, 0.0f, -0.5f, 0.0f, 1.5f, 3.0f, 4.5f, 6.0f, 7.0f),
                tubeWarmth = 0.20f,
                superResClarity = 0.95f,
                crossfeedEnabled = true,
                crossfeedLevel = 0.35f,
                isFactoryDefault = true
            ),
            EqualizerPresetEntity.fromGainsList(
                name = "Analog Tube Tape",
                genre = "Warmth",
                gains = listOf(3.0f, 2.5f, 2.0f, 1.0f, 0.5f, 0.0f, -0.5f, -1.0f, -2.0f, -3.0f),
                tubeWarmth = 0.85f,
                superResClarity = 0.30f,
                crossfeedEnabled = true,
                crossfeedLevel = 0.50f,
                isFactoryDefault = true
            ),
            EqualizerPresetEntity.fromGainsList(
                name = "Crystal Vocals",
                genre = "Vocal Pop",
                gains = listOf(-2.0f, -1.5f, -0.5f, 1.0f, 3.0f, 4.0f, 3.5f, 2.5f, 2.0f, 1.5f),
                tubeWarmth = 0.25f,
                superResClarity = 0.85f,
                crossfeedEnabled = true,
                crossfeedLevel = 0.30f,
                isFactoryDefault = true
            ),
            EqualizerPresetEntity.fromGainsList(
                name = "Heavy Metal Shred",
                genre = "Heavy Metal",
                gains = listOf(5.0f, 3.5f, -1.0f, -2.5f, -2.0f, 0.5f, 3.0f, 4.5f, 4.0f, 4.5f),
                tubeWarmth = 0.70f,
                superResClarity = 0.70f,
                crossfeedEnabled = false,
                isFactoryDefault = true
            ),
            EqualizerPresetEntity.fromGainsList(
                name = "Lo-Fi Chill Rain",
                genre = "Lo-Fi Chill",
                gains = listOf(4.0f, 3.0f, 1.0f, -0.5f, 0.0f, -1.0f, -2.0f, -3.0f, -4.5f, -6.0f),
                tubeWarmth = 0.80f,
                superResClarity = 0.20f,
                crossfeedEnabled = true,
                crossfeedLevel = 0.60f,
                isFactoryDefault = true
            )
        )
    }
}
