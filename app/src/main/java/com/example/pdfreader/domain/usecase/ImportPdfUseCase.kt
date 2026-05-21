package com.example.pdfreader.domain.usecase

import android.net.Uri
import com.example.pdfreader.domain.model.Book
import com.example.pdfreader.domain.repository.BookRepository
import javax.inject.Inject

class ImportPdfUseCase @Inject constructor(
    private val bookRepository: BookRepository
) {
    suspend operator fun invoke(uri: Uri): Result<Book> {
        return bookRepository.importPdf(uri)
    }
}
