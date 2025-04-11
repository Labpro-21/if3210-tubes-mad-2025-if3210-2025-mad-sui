package com.vibecoder.purrytify.data.repository

import android.net.Uri
import com.vibecoder.purrytify.data.local.dao.SongDao
import com.vibecoder.purrytify.data.local.model.SongEntity
import com.vibecoder.purrytify.util.Resource
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class SongRepository @Inject constructor(private val songDao: SongDao) {

    fun getAllSongs(): Flow<List<SongEntity>> {
        return songDao.getAllSongs()
    }

    fun getLikedSongs(): Flow<List<SongEntity>> {
        return songDao.getLikedSongs()
    }

    suspend fun addSong(song: SongEntity): Resource<Unit> {
        return try {
            //  OWASP M4 for input validation
            val validationResult = validateSongData(song)
            if (validationResult != null) {
                return Resource.Error(validationResult)
            }

            if (!isValidUri(song.filePathUri)) {
                return Resource.Error("Invalid audio file URI format")
            }

            if (song.coverArtUri != null && !isValidUri(song.coverArtUri)) {
                return Resource.Error("Invalid cover art URI format")
            }

            if (song.duration > MAX_SONG_DURATION_MS) {
                return Resource.Error("Song exceeds maximum allowed duration")
            }

            songDao.insertSong(song)
            Resource.Success(Unit)
        } catch (e: IOException) {
            Resource.Error("Couldn't save song to database: ${e.localizedMessage}")
        } catch (e: Exception) {
            Resource.Error(
                    "An unexpected error occurred while saving the song: ${e.localizedMessage}"
            )
        }
    }

    suspend fun updateSong(song: SongEntity): Resource<Unit> {
        return try {
            // OWASP M4 for input validation
            if (song.id <= 0L) {
                return Resource.Error("Cannot update song with invalid ID.")
            }

            val validationResult = validateSongData(song)
            if (validationResult != null) {
                return Resource.Error(validationResult)
            }

            // URI validation
            if (!isValidUri(song.filePathUri)) {
                return Resource.Error("Invalid audio file URI format")
            }

            if (song.coverArtUri != null && !isValidUri(song.coverArtUri)) {
                return Resource.Error("Invalid cover art URI format")
            }

            // Verify the song exists before updating
            val existingSong = songDao.getSongById(song.id)
            if (existingSong == null) {
                return Resource.Error("Cannot update non-existent song with ID: ${song.id}")
            }

            songDao.updateSong(song)
            Resource.Success(Unit)
        } catch (e: IOException) {
            Resource.Error("Couldn't update song in database: ${e.localizedMessage}")
        } catch (e: Exception) {
            Resource.Error(
                    "An unexpected error occurred while updating the song: ${e.localizedMessage}"
            )
        }
    }

    suspend fun deleteSong(songId: Long): Resource<Unit> {
        return try {
            if (songId <= 0L) {
                return Resource.Error("Invalid song ID.")
            }

            val existingSong = songDao.getSongById(songId)
            if (existingSong == null) {
                return Resource.Error("Cannot delete non-existent song with ID: $songId")
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

            val existingSong = songDao.getSongById(songId)
            if (existingSong == null) {
                return Resource.Error(
                        "Cannot update like status for non-existent song with ID: $songId"
                )
            }

            songDao.updateLikeStatus(songId, isLiked)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("Failed to update like status: ${e.localizedMessage}")
        }
    }

    // OWASP M4
    private fun validateSongData(song: SongEntity): String? {
        // Title validation
        if (song.title.isBlank()) {
            return "Song title cannot be empty"
        }

        if (song.title.length > MAX_TITLE_LENGTH) {
            return "Song title exceeds maximum allowed length (${MAX_TITLE_LENGTH} characters)"
        }

        // Artist validation
        if (song.artist.isBlank()) {
            return "Song artist cannot be empty"
        }

        if (song.artist.length > MAX_ARTIST_LENGTH) {
            return "Artist name exceeds maximum allowed length (${MAX_ARTIST_LENGTH} characters)"
        }

        // File path validation
        if (song.filePathUri.isBlank()) {
            return "Song file path cannot be empty"
        }

        // Duration validation
        if (song.duration <= 0) {
            return "Song duration must be positive"
        }

        return null
    }

    private fun isValidUri(uriString: String): Boolean {
        return try {
            val uri = Uri.parse(uriString)

            // Check if the URI has a scheme and path
            uri.scheme != null &&
                    uri.scheme!!.isNotEmpty() &&
                    (uri.path != null && uri.path!!.isNotEmpty())
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        // OWASP M4
        private const val MAX_ARTIST_LENGTH = 100
        private const val MAX_SONG_DURATION_MS = 30 * 60 * 1000L // 30 minutes
        private const val MAX_TITLE_LENGTH = 100
    }
}
