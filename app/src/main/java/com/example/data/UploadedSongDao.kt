package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UploadedSongDao {
    @Query("SELECT * FROM uploaded_songs ORDER BY addedTimestamp DESC")
    fun getAllSongs(): Flow<List<UploadedSongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: UploadedSongEntity): Long

    @Delete
    suspend fun deleteSong(song: UploadedSongEntity)

    @Query("DELETE FROM uploaded_songs WHERE id = :id")
    suspend fun deleteById(id: Long)
}
