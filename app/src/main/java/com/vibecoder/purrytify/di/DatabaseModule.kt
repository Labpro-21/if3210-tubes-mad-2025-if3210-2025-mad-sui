package com.vibecoder.purrytify.di

import android.content.Context
import androidx.room.Room
import com.vibecoder.purrytify.data.local.AppDatabase
import com.vibecoder.purrytify.data.local.dao.SongDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
                .fallbackToDestructiveMigration()
                .build()
    }

    @Provides
    @Singleton
    fun provideSongDao(appDatabase: AppDatabase): SongDao {

        return appDatabase.songDao()
    }
}
