package com.example.pdfreader.domain.usecase

import com.example.pdfreader.domain.model.Book
import com.example.pdfreader.domain.repository.BookRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchBooksUseCase @Inject constructor(
    private val bookRepository: BookRepository
) {
    operator fun invoke(query: String): Flow<List<Book>> {
        return bookRepository.searchBooks(query)
    }
}
