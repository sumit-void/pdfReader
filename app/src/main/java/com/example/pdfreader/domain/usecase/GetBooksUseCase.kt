package com.example.pdfreader.domain.usecase

import com.example.pdfreader.domain.model.Book
import com.example.pdfreader.domain.model.LibrarySortMode
import com.example.pdfreader.domain.repository.BookRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetBooksUseCase @Inject constructor(
    private val bookRepository: BookRepository
) {
    operator fun invoke(sortMode: LibrarySortMode = LibrarySortMode.RECENT): Flow<List<Book>> {
        return bookRepository.getAllBooks().map { books ->
            when (sortMode) {
                LibrarySortMode.RECENT -> books.sortedByDescending { it.lastOpened }
                LibrarySortMode.NAME -> books.sortedBy { it.title.lowercase() }
                LibrarySortMode.SIZE -> books.sortedByDescending { it.fileSize }
                LibrarySortMode.PROGRESS -> books.sortedByDescending { it.progress }
            }
        }
    }
}
