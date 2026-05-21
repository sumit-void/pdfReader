package com.example.pdfreader.domain.repository

import android.net.Uri
import com.example.pdfreader.domain.model.Book
import kotlinx.coroutines.flow.Flow

interface BookRepository {
    fun getAllBooks(): Flow<List<Book>>
    fun searchBooks(query: String): Flow<List<Book>>
    suspend fun getBookById(id: Long): Book?
    fun observeBook(id: Long): Flow<Book?>
    suspend fun getLastOpenedBook(): Book?
    suspend fun importPdf(uri: Uri): Result<Book>
    suspend fun deleteBook(book: Book)
    suspend fun updateReadingProgress(bookId: Long, page: Int)
    suspend fun addReadingTime(bookId: Long, durationMs: Long)
    suspend fun getBookCount(): Int
    suspend fun getTotalPagesRead(): Int
    suspend fun extractPageText(filePath: String, pageIndex: Int): String
    suspend fun cleanupOrphanedFiles()
}
