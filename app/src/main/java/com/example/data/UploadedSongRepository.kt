package com.example.data

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class UploadedSongRepository(
    private val uploadedSongDao: UploadedSongDao,
    private val uploadManager: SongUploadManager
) {

    val allUploadedSongs: Flow<List<UploadedSongEntity>> = uploadedSongDao.getAllSongs()

    suspend fun uploadFromUri(uri: Uri): Result<UploadedSongEntity> = withContext(Dispatchers.IO) {
        val result = uploadManager.importAudioFromUri(uri)
        result.onSuccess { song ->
            val id = uploadedSongDao.insertSong(song)
            return@withContext Result.success(song.copy(id = id))
        }
        return@withContext result
    }

    suspend fun createDemoCyberdeckTrack(): Result<UploadedSongEntity> = withContext(Dispatchers.IO) {
        val result = uploadManager.generateDemoCyberdeckTrack()
        result.onSuccess { song ->
            val id = uploadedSongDao.insertSong(song)
            return@withContext Result.success(song.copy(id = id))
        }
        return@withContext result
    }

    suspend fun createDemoLoFiTrack(): Result<UploadedSongEntity> = withContext(Dispatchers.IO) {
        val result = uploadManager.generateLoFiTrack()
        result.onSuccess { song ->
            val id = uploadedSongDao.insertSong(song)
            return@withContext Result.success(song.copy(id = id))
        }
        return@withContext result
    }

    suspend fun uploadFromUrl(url: String, title: String? = null): Result<UploadedSongEntity> = withContext(Dispatchers.IO) {
        val result = uploadManager.importAudioFromUrl(url, title)
        result.onSuccess { song ->
            val id = uploadedSongDao.insertSong(song)
            return@withContext Result.success(song.copy(id = id))
        }
        return@withContext result
    }

    suspend fun uploadMultipleFromUris(uris: List<Uri>): List<UploadedSongEntity> = withContext(Dispatchers.IO) {
        val added = mutableListOf<UploadedSongEntity>()
        for (uri in uris) {
            val res = uploadManager.importAudioFromUri(uri)
            res.getOrNull()?.let { song ->
                val id = uploadedSongDao.insertSong(song)
                added.add(song.copy(id = id))
            }
        }
        added
    }

    suspend fun deleteSong(song: UploadedSongEntity) = withContext(Dispatchers.IO) {
        uploadManager.deleteSongFile(song.filePath)
        uploadedSongDao.deleteById(song.id)
    }
}
