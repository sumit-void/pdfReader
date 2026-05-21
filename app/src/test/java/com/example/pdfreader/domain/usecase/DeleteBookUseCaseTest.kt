package com.example.pdfreader.domain.usecase

import com.example.pdfreader.domain.model.Book
import com.example.pdfreader.domain.repository.BookRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class DeleteBookUseCaseTest {

    private val bookRepository: BookRepository = mockk()
    private lateinit var deleteBookUseCase: DeleteBookUseCase

    @Before
    fun setUp() {
        deleteBookUseCase = DeleteBookUseCase(bookRepository)
    }

    @Test
    fun `invoke calls deleteBook on repository with correct book`() = runTest {
        val book = Book(id = 1, title = "To Be Deleted")
        coEvery { bookRepository.deleteBook(book) } just runs

        deleteBookUseCase(book)

        coVerify(exactly = 1) { bookRepository.deleteBook(book) }
    }
}
