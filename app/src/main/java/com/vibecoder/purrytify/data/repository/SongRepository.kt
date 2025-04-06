package com.vibecoder.purrytify.data.repository

import com.vibecoder.purrytify.data.local.dao.SongDao
import com.vibecoder.purrytify.data.local.model.SongEntity
import com.vibecoder.purrytify.util.Resource
import kotlinx.coroutines.flow.Flow
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class SongRepository @Inject constructor(
    private val songDao: SongDao
) {

    
    fun getAllSongs(): Flow<List<SongEntity>> {
        return songDao.getAllSongs()
    }

    fun getLikedSongs(): Flow<List<SongEntity>> {
        return songDao.getLikedSongs()
    }

    suspend fun addSong(song: SongEntity): Resource<Unit> {
        return try {

            if (song.filePathUri.isBlank()) {
                return Resource.Error("Song file path cannot be empty.")
            }
            if (song.title.isBlank()) {
                return Resource.Error("Song title cannot be empty.")
            }
            if (song.artist.isBlank()) {
                return Resource.Error("Song artist cannot be empty.")
            }
            if (song.duration <= 0) {
                return Resource.Error("Song duration must be positive.")
            }

            songDao.insertSong(song)
            Resource.Success(Unit)
        } catch (e: IOException) {
            Resource.Error("Couldn't save song to database: ${e.localizedMessage}")
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred while saving the song: ${e.localizedMessage}")
        }
    }

    suspend fun updateSong(song: SongEntity): Resource<Unit> {
        return try {
            if (song.id == 0L) {
                return Resource.Error("Cannot update song with invalid ID.")
            }
            if (song.filePathUri.isBlank()) {
                return Resource.Error("Song file path cannot be empty.")
            }

            songDao.updateSong(song)
            Resource.Success(Unit)
        } catch (e: IOException) {
            Resource.Error("Couldn't update song in database: ${e.localizedMessage}")
        } catch (e: Exception) {
            Resource.Error("An unexpected error occurred while updating the song: ${e.localizedMessage}")
        }
    }


    suspend fun deleteSong(songId: Long): Resource<Unit> {
        return try {
            if (songId <= 0L) {
                return Resource.Error("Invalid song ID.")
            }
            songDao.deleteSongById(songId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("Failed to delete song: ${e.localizedMessage}")
        }
    }

    suspend fun getSongById(songId: Long): Resource<SongEntity?> {
        return try {
            if (songId <= 0L) {
                return Resource.Error("Invalid song ID.")
            }
            val entity = songDao.getSongById(songId)
            Resource.Success(entity)
        } catch (e: Exception) {
            Resource.Error("Failed to get song: ${e.localizedMessage}")
        }
    }

    suspend fun updateLikeStatus(songId: Long, isLiked: Boolean): Resource<Unit> {
        return try {
            if (songId <= 0L) {
                return Resource.Error("Invalid song ID.")
            }
            songDao.updateLikeStatus(songId, isLiked)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("Failed to update like status: ${e.localizedMessage}")
        }
    }
}