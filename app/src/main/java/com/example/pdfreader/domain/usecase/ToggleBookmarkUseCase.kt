package com.example.pdfreader.domain.usecase

import com.example.pdfreader.domain.repository.BookmarkRepository
import javax.inject.Inject

class ToggleBookmarkUseCase @Inject constructor(
    private val bookmarkRepository: BookmarkRepository
) {
    suspend operator fun invoke(bookId: Long, page: Int): Boolean {
        return bookmarkRepository.toggleBookmark(bookId, page)
    }
}
