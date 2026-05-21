package com.example.pdfreader.di

import android.content.Context
import androidx.room.Room
import com.example.pdfreader.data.local.PaperbackDatabase
import com.example.pdfreader.data.local.dao.BookDao
import com.example.pdfreader.data.local.dao.BookmarkDao
import com.example.pdfreader.data.local.dao.HighlightDao
import com.example.pdfreader.data.local.dao.ReadingSessionDao
import com.example.pdfreader.data.local.dao.StreakDao
import com.example.pdfreader.data.local.dao.GoalDao
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
    fun provideDatabase(
        @ApplicationContext context: Context
    ): PaperbackDatabase {
        return Room.databaseBuilder(
            context,
            PaperbackDatabase::class.java,
            PaperbackDatabase.DATABASE_NAME
        )
            .addMigrations(PaperbackDatabase.MIGRATION_1_2)
            .build()
    }

    @Provides
    fun provideBookDao(database: PaperbackDatabase): BookDao = database.bookDao()

    @Provides
    fun provideBookmarkDao(database: PaperbackDatabase): BookmarkDao = database.bookmarkDao()

    @Provides
    fun provideHighlightDao(database: PaperbackDatabase): HighlightDao = database.highlightDao()

    @Provides
    fun provideReadingSessionDao(database: PaperbackDatabase): ReadingSessionDao = database.readingSessionDao()

    @Provides
    fun provideStreakDao(database: PaperbackDatabase): StreakDao = database.streakDao()

    @Provides
    fun provideGoalDao(database: PaperbackDatabase): GoalDao = database.goalDao()
}
