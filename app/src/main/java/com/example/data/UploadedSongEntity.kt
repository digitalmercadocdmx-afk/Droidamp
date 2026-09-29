package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "uploaded_songs")
@Serializable
data class UploadedSongEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val artist: String = "Local Audio",
    val filePath: String,
    val durationMs: Long = 0L,
    val fileSizeFormatted: String = "",
    val mimeType: String = "audio/mpeg",
    val addedTimestamp: Long = System.currentTimeMillis()
)
