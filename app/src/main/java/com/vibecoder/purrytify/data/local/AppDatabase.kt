package com.vibecoder.purrytify.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.vibecoder.purrytify.data.local.dao.SongDao
import com.vibecoder.purrytify.data.local.model.SongEntity

@Database(entities = [SongEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao

    companion object {
        const val DATABASE_NAME = "purrytify_database"
    }
}