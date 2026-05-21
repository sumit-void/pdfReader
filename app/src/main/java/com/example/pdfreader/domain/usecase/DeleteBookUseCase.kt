package com.example.pdfreader.domain.usecase

import com.example.pdfreader.domain.model.Book
import com.example.pdfreader.domain.repository.BookRepository
import javax.inject.Inject

class DeleteBookUseCase @Inject constructor(
    private val bookRepository: BookRepository
) {
    suspend operator fun invoke(book: Book) {
        bookRepository.deleteBook(book)
    }
}
