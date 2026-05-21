package com.example.pdfreader.data.local.dao

import androidx.room.*
import com.example.pdfreader.data.local.entity.BookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {

    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY pageNumber ASC")
    fun getBookmarksForBook(bookId: Long): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId AND pageNumber = :page LIMIT 1")
    suspend fun getBookmarkForPage(bookId: Long, page: Int): BookmarkEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE bookId = :bookId AND pageNumber = :page)")
    fun isPageBookmarked(bookId: Long, page: Int): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Delete
    suspend fun deleteBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE bookId = :bookId AND pageNumber = :page")
    suspend fun deleteBookmarkByPage(bookId: Long, page: Int)

    @Query("SELECT COUNT(*) FROM bookmarks WHERE bookId = :bookId")
    suspend fun getBookmarkCount(bookId: Long): Int
}
