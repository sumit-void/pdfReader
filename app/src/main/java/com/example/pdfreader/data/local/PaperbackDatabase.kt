package com.example.pdfreader.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.pdfreader.data.local.dao.BookDao
import com.example.pdfreader.data.local.dao.BookmarkDao
import com.example.pdfreader.data.local.dao.HighlightDao
import com.example.pdfreader.data.local.dao.ReadingSessionDao
import com.example.pdfreader.data.local.dao.StreakDao
import com.example.pdfreader.data.local.dao.GoalDao
import com.example.pdfreader.data.local.entity.BookEntity
import com.example.pdfreader.data.local.entity.BookmarkEntity
import com.example.pdfreader.data.local.entity.HighlightEntity
import com.example.pdfreader.data.local.entity.ReadingSessionEntity
import com.example.pdfreader.data.local.entity.StreakEntity
import com.example.pdfreader.data.local.entity.GoalEntity

@Database(
    entities = [
        BookEntity::class,
        BookmarkEntity::class,
        HighlightEntity::class,
        ReadingSessionEntity::class,
        StreakEntity::class,
        GoalEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class PaperbackDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun highlightDao(): HighlightDao
    abstract fun readingSessionDao(): ReadingSessionDao
    abstract fun streakDao(): StreakDao
    abstract fun goalDao(): GoalDao

    companion object {
        const val DATABASE_NAME = "paperback_db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `streaks` (
                        `date` TEXT NOT NULL, 
                        `pagesRead` INTEGER NOT NULL, 
                        `timeReadMs` INTEGER NOT NULL, 
                        PRIMARY KEY(`date`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `goals` (
                        `id` INTEGER NOT NULL, 
                        `dailyPagesGoal` INTEGER NOT NULL, 
                        `dailyTimeGoalMinutes` INTEGER NOT NULL, 
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
