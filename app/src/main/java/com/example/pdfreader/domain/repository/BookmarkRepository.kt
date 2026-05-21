package com.example.pdfreader.domain.repository

import com.example.pdfreader.domain.model.Bookmark
import kotlinx.coroutines.flow.Flow

interface BookmarkRepository {
    fun getBookmarksForBook(bookId: Long): Flow<List<Bookmark>>
    fun isPageBookmarked(bookId: Long, page: Int): Flow<Boolean>
    suspend fun toggleBookmark(bookId: Long, page: Int): Boolean
    suspend fun deleteBookmark(bookmark: Bookmark)
}
