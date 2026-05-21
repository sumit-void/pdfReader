package com.example.pdfreader.data.local.dao

import androidx.room.*
import com.example.pdfreader.data.local.entity.BookPageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookPageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPage(page: BookPageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPages(pages: List<BookPageEntity>)

    @Query("SELECT * FROM book_pages WHERE bookId = :bookId AND pageIndex = :pageIndex LIMIT 1")
    suspend fun getPageText(bookId: Long, pageIndex: Int): BookPageEntity?

    @Query("SELECT COUNT(*) FROM book_pages WHERE bookId = :bookId")
    suspend fun getIndexedPageCount(bookId: Long): Int

    @Query("SELECT pageIndex FROM book_pages WHERE bookId = :bookId ORDER BY pageIndex ASC")
    suspend fun getIndexedPageIndices(bookId: Long): List<Int>

    @Query("""
        SELECT bp.* FROM book_pages bp
        JOIN book_pages_fts fts ON bp.id = fts.rowid
        WHERE bp.bookId = :bookId AND book_pages_fts MATCH :query
    """)
    suspend fun searchInBook(bookId: Long, query: String): List<BookPageEntity>

    @Query("""
        SELECT bp.* FROM book_pages bp
        JOIN book_pages_fts fts ON bp.id = fts.rowid
        WHERE book_pages_fts MATCH :query
    """)
    suspend fun searchAllBooks(query: String): List<BookPageEntity>

    @Query("DELETE FROM book_pages WHERE bookId = :bookId")
    suspend fun deletePagesForBook(bookId: Long)
}
