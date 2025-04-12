package com.vibecoder.purrytify.data.local.dao

import androidx.room.*
import com.vibecoder.purrytify.data.local.model.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongEntity): Long

    @Update
    suspend fun updateSong(song: SongEntity)

    @Delete
    suspend fun deleteSong(song: SongEntity)

    @Query("DELETE FROM songs WHERE id = :songId")
    suspend fun deleteSongById(songId: Long)

    @Query("SELECT * FROM songs WHERE userEmail = :userEmail ORDER BY createdAt DESC")
    fun getAllSongs(userEmail: String): Flow<List<SongEntity>>


    @Query("SELECT * FROM songs WHERE userEmail = :userEmail AND isLiked = 1 ORDER BY createdAt DESC")
    fun getLikedSongs(userEmail: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id = :songId")
    suspend fun getSongById(songId: Long): SongEntity?

    @Query("UPDATE songs SET isLiked = :isLiked WHERE id = :songId")
    suspend fun updateLikeStatus(songId: Long, isLiked: Boolean)

    @Query("UPDATE songs SET isListened = 1 WHERE id = :songId AND userEmail = :userEmail AND isListened = 0")
    suspend fun markAsListened(songId: Long, userEmail: String): Int

    @Query("SELECT COUNT(*) FROM songs WHERE userEmail = :userEmail")
    fun getSongCount(userEmail: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM songs WHERE userEmail = :userEmail AND isLiked = 1")
    fun getLikedSongCount(userEmail: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM songs WHERE userEmail = :userEmail AND isListened = 1")
    fun getListenedSongCount(userEmail: String): Flow<Int>
}