package com.example.pdfreader.domain.usecase

import com.example.pdfreader.domain.model.Book
import com.example.pdfreader.domain.repository.BookRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SearchBooksUseCaseTest {

    private val bookRepository: BookRepository = mockk()
    private lateinit var searchBooksUseCase: SearchBooksUseCase

    @Before
    fun setUp() {
        searchBooksUseCase = SearchBooksUseCase(bookRepository)
    }

    @Test
    fun `invoke with query returns matching books from repository`() = runTest {
        val query = "Compose"
        val expectedBooks = listOf(
            Book(id = 1, title = "Jetpack Compose Guide"),
            Book(id = 2, title = "Advanced Compose")
        )
        every { bookRepository.searchBooks(query) } returns flowOf(expectedBooks)

        val result = searchBooksUseCase(query).first()

        assertEquals(expectedBooks, result)
        verify(exactly = 1) { bookRepository.searchBooks(query) }
    }
}
