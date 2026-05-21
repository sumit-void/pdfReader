package com.example.pdfreader.data.repository

import com.example.pdfreader.data.local.dao.BookmarkDao
import com.example.pdfreader.data.local.entity.BookmarkEntity
import com.example.pdfreader.domain.model.Bookmark
import com.example.pdfreader.domain.repository.BookmarkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookmarkRepositoryImpl @Inject constructor(
    private val bookmarkDao: BookmarkDao
) : BookmarkRepository {

    override fun getBookmarksForBook(bookId: Long): Flow<List<Bookmark>> {
        return bookmarkDao.getBookmarksForBook(bookId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun isPageBookmarked(bookId: Long, page: Int): Flow<Boolean> {
        return bookmarkDao.isPageBookmarked(bookId, page)
    }

    override suspend fun toggleBookmark(bookId: Long, page: Int): Boolean {
        val existing = bookmarkDao.getBookmarkForPage(bookId, page)
        return if (existing != null) {
            bookmarkDao.deleteBookmark(existing)
            false
        } else {
            bookmarkDao.insertBookmark(
                BookmarkEntity(bookId = bookId, pageNumber = page)
            )
            true
        }
    }

    override suspend fun deleteBookmark(bookmark: Bookmark) {
        bookmarkDao.deleteBookmark(
            BookmarkEntity(
                id = bookmark.id,
                bookId = bookmark.bookId,
                pageNumber = bookmark.pageNumber,
                note = bookmark.note,
                dateCreated = bookmark.dateCreated
            )
        )
    }

    private fun BookmarkEntity.toDomainModel(): Bookmark {
        return Bookmark(
            id = id,
            bookId = bookId,
            pageNumber = pageNumber,
            note = note,
            dateCreated = dateCreated
        )
    }
}
