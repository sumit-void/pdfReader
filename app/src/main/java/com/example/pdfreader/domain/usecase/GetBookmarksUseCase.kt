package com.example.pdfreader.domain.usecase

import com.example.pdfreader.domain.model.Bookmark
import com.example.pdfreader.domain.repository.BookmarkRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetBookmarksUseCase @Inject constructor(
    private val bookmarkRepository: BookmarkRepository
) {
    operator fun invoke(bookId: Long): Flow<List<Bookmark>> {
        return bookmarkRepository.getBookmarksForBook(bookId)
    }
}
