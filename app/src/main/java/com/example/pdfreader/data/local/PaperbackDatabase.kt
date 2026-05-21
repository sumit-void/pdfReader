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
        GoalEntity::class,
        com.example.pdfreader.data.local.entity.BookPageEntity::class,
        com.example.pdfreader.data.local.entity.BookPageFtsEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class PaperbackDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun highlightDao(): HighlightDao
    abstract fun readingSessionDao(): ReadingSessionDao
    abstract fun streakDao(): StreakDao
    abstract fun goalDao(): GoalDao
    abstract fun bookPageDao(): com.example.pdfreader.data.local.dao.BookPageDao

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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `book_pages` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `bookId` INTEGER NOT NULL, 
                        `pageIndex` INTEGER NOT NULL, 
                        `pageText` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE VIRTUAL TABLE IF NOT EXISTS `book_pages_fts` USING fts5(
                        `pageText`, 
                        content=`book_pages`, 
                        content_rowid=`id`
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_book_pages_fts_BEFORE_UPDATE 
                    BEFORE UPDATE ON `book_pages` BEGIN 
                        DELETE FROM `book_pages_fts` WHERE rowid = OLD.`id`; 
                    END
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_book_pages_fts_BEFORE_DELETE 
                    BEFORE DELETE ON `book_pages` BEGIN 
                        DELETE FROM `book_pages_fts` WHERE rowid = OLD.`id`; 
                    END
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_book_pages_fts_AFTER_UPDATE 
                    AFTER UPDATE ON `book_pages` BEGIN 
                        INSERT INTO `book_pages_fts`(rowid, `pageText`) VALUES (NEW.`id`, NEW.`pageText`); 
                    END
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_book_pages_fts_AFTER_INSERT 
                    AFTER INSERT ON `book_pages` BEGIN 
                        INSERT INTO `book_pages_fts`(rowid, `pageText`) VALUES (NEW.`id`, NEW.`pageText`); 
                    END
                    """.trimIndent()
                )
            }
        }
    }
}
