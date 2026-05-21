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
        net.sqlcipher.database.SQLiteDatabase.loadLibs(context)
        val passphrase = com.example.pdfreader.util.SecurityKeyManager.getDatabaseKey(context)
        val factory = net.sqlcipher.database.SupportFactory(passphrase)

        val db = Room.databaseBuilder(
            context,
            PaperbackDatabase::class.java,
            PaperbackDatabase.DATABASE_NAME
        )
            .openHelperFactory(factory)
            .addMigrations(PaperbackDatabase.MIGRATION_1_2, PaperbackDatabase.MIGRATION_2_3)
            .build()

        return try {
            // Force-open the database and verify decryption works
            db.openHelper.writableDatabase
            db
        } catch (e: Exception) {
            try {
                db.close()
            } catch (closeEx: Exception) {
                // Ignore closing exceptions
            }
            context.deleteDatabase(PaperbackDatabase.DATABASE_NAME)
            com.example.pdfreader.util.SecurityKeyManager.resetDatabaseKey(context)
            
            val newPassphrase = com.example.pdfreader.util.SecurityKeyManager.getDatabaseKey(context)
            val newFactory = net.sqlcipher.database.SupportFactory(newPassphrase)
            val freshDb = Room.databaseBuilder(
                context,
                PaperbackDatabase::class.java,
                PaperbackDatabase.DATABASE_NAME
            )
                .openHelperFactory(newFactory)
                .addMigrations(PaperbackDatabase.MIGRATION_1_2, PaperbackDatabase.MIGRATION_2_3)
                .build()
            try {
                freshDb.openHelper.writableDatabase
            } catch (e2: Exception) {
                // Ultimate fallback: if encryption is completely broken on this device,
                // fall back to an unencrypted Room database to avoid any crash
                return Room.databaseBuilder(
                    context,
                    PaperbackDatabase::class.java,
                    PaperbackDatabase.DATABASE_NAME
                )
                    .addMigrations(PaperbackDatabase.MIGRATION_1_2, PaperbackDatabase.MIGRATION_2_3)
                    .build()
            }
            freshDb
        }
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

    @Provides
    @Singleton
    fun provideBookPageDao(database: PaperbackDatabase): com.example.pdfreader.data.local.dao.BookPageDao = database.bookPageDao()
}
