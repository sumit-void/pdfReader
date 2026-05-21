package com.example.pdfreader.data.local.dao

import androidx.room.*
import com.example.pdfreader.data.local.entity.BookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    @Query("SELECT * FROM books ORDER BY lastOpened DESC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBookById(id: Long): BookEntity?

    @Query("SELECT * FROM books WHERE id = :id")
    fun observeBookById(id: Long): Flow<BookEntity?>

    @Query("SELECT * FROM books ORDER BY lastOpened DESC LIMIT 1")
    suspend fun getLastOpenedBook(): BookEntity?

    @Query("SELECT * FROM books WHERE title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%'")
    fun searchBooks(query: String): Flow<List<BookEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity): Long

    @Update
    suspend fun updateBook(book: BookEntity)

    @Delete
    suspend fun deleteBook(book: BookEntity)

    @Query("UPDATE books SET currentPage = :page, lastOpened = :timestamp WHERE id = :bookId")
    suspend fun updateReadingProgress(bookId: Long, page: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE books SET totalReadTimeMs = totalReadTimeMs + :durationMs WHERE id = :bookId")
    suspend fun addReadingTime(bookId: Long, durationMs: Long)

    @Query("SELECT * FROM books WHERE filePath = :path LIMIT 1")
    suspend fun getBookByPath(path: String): BookEntity?

    @Query("SELECT COUNT(*) FROM books")
    suspend fun getBookCount(): Int

    @Query("SELECT SUM(currentPage) FROM books")
    suspend fun getTotalPagesRead(): Int?
}
