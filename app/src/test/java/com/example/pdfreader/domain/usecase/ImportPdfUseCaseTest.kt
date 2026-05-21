package com.example.pdfreader.domain.usecase

import android.net.Uri
import com.example.pdfreader.domain.model.Book
import com.example.pdfreader.domain.repository.BookRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ImportPdfUseCaseTest {

    private val bookRepository: BookRepository = mockk()
    private val mockUri: Uri = mockk()
    private lateinit var importPdfUseCase: ImportPdfUseCase

    @Before
    fun setUp() {
        importPdfUseCase = ImportPdfUseCase(bookRepository)
    }

    @Test
    fun `invoke with valid uri returns success Result with Book`() = runTest {
        val expectedBook = Book(id = 1, title = "New Book", filePath = "/path/to/book.pdf")
        coEvery { bookRepository.importPdf(mockUri) } returns Result.success(expectedBook)

        val result = importPdfUseCase(mockUri)

        assertTrue(result.isSuccess)
        assertEquals(expectedBook, result.getOrNull())
        coVerify(exactly = 1) { bookRepository.importPdf(mockUri) }
    }

    @Test
    fun `invoke with invalid uri returns failure Result`() = runTest {
        val expectedException = Exception("Failed to read PDF")
        coEvery { bookRepository.importPdf(mockUri) } returns Result.failure(expectedException)

        val result = importPdfUseCase(mockUri)

        assertTrue(result.isFailure)
        assertEquals(expectedException, result.exceptionOrNull())
        coVerify(exactly = 1) { bookRepository.importPdf(mockUri) }
    }
}
