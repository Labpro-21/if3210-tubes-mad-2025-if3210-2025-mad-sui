package com.vibecoder.purrytify.data.repository

import android.net.Uri
import android.util.Log
import com.vibecoder.purrytify.data.local.dao.SongDao
import com.vibecoder.purrytify.data.local.dto.AddSongRequest
import com.vibecoder.purrytify.data.local.dto.UpdateSongRequest
import com.vibecoder.purrytify.data.local.model.SongEntity
import com.vibecoder.purrytify.util.Resource
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

@Singleton
class SongRepository @Inject constructor(private val songDao: SongDao, private  val authRepository: AuthRepository) {
    private val TAG = "SongRepository"
    private val userEmail: String get() = authRepository.getCurrentUserEmail() ?: ""
    fun getAllSongs(): Flow<List<SongEntity>> {
        Log.d(TAG, "Getting all songs for user: $userEmail")
        return songDao.getAllSongs(userEmail)
            .catch { e ->
                Log.e(TAG, "Error getting all songs for user $userEmail", e)
                emit(emptyList())
            }
    }

    fun getLikedSongs(): Flow<List<SongEntity>> {
        Log.d(TAG, "Getting liked songs for user: $userEmail")
        return songDao.getLikedSongs(userEmail)
            .catch { e ->
                Log.e(TAG, "Error getting liked songs for user $userEmail", e)
                emit(emptyList())
            }
    }
    fun getLikedSongCount(): Flow<Int> {
        Log.d(TAG, "Getting liked songs count for user: $userEmail")
        return songDao.getLikedSongCount(userEmail)
            .catch { e ->
                Log.e(TAG, "Error getting liked songs count for user $userEmail", e)
                emit(0)
            }
    }

    suspend fun addSong(songReq: AddSongRequest): Resource<Unit> {
        return try {

            val song = SongEntity(
                title = songReq.title,
                artist = songReq.artist,
                filePathUri = songReq.filePathUri,
                coverArtUri = songReq.coverArtUri,
                duration = songReq.duration,
                isLiked = songReq.isLiked,
                userEmail = userEmail
            )
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

    suspend fun updateSong(songReq: UpdateSongRequest): Resource<Unit> {
        return try {
            // OWASP M4 for input validation
            val song = SongEntity(
                id = songReq.id,
                title = songReq.title,
                artist = songReq.artist,
                filePathUri = songReq.filePathUri,
                coverArtUri = songReq.coverArtUri,
                duration = songReq.duration,
                isLiked = songReq.isLiked,
                userEmail = userEmail
            )
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


            val existingSong = songDao.getSongById(song.id)
                ?: return Resource.Error("Cannot update non-existent or unauthorized song with ID: ${song.id} for user ${song.userEmail}")



            songDao.updateSong(song)
            Log.i(TAG, "Song '${song.title}' (ID: ${song.id}) updated successfully for user ${song.userEmail}")
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
    suspend fun markAsListened(songId: Long): Resource<Unit> {
        return try {
            if (songId <= 0L || userEmail.isBlank()) {
                return Resource.Error("Invalid song ID or User Email.")
            }
            val rowsAffected = songDao.markAsListened(songId, userEmail)
            if (rowsAffected > 0) {
                Log.d(TAG, "Marked song ID $songId as listened for user $userEmail")
            } else {
                Log.d(TAG, "Song ID $songId was already marked as listened or not found for user $userEmail")
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking song ID $songId as listened for user $userEmail", e)
            Resource.Error("Failed to update listened status: ${e.localizedMessage}")
        }
    }
    fun getSongCount(): Flow<Int> {
        Log.d(TAG, "Getting song count for user: $userEmail")
        return songDao.getSongCount(userEmail)
            .catch { e ->
                Log.e(TAG, "Error getting song count for user $userEmail", e)
                emit(0)
            }
    }
    fun getListenedSongCount(): Flow<Int> {
        Log.d(TAG, "Getting count of listened songs for user: $userEmail")
        return songDao.getListenedSongCount(userEmail)
            .catch { e ->
                Log.e(TAG, "Error getting listened song count for user $userEmail", e)
                emit(0)
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
