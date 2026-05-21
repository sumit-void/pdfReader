package com.example.pdfreader.domain.usecase

import com.example.pdfreader.domain.model.Book
import com.example.pdfreader.domain.model.LibrarySortMode
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

class GetBooksUseCaseTest {

    private val bookRepository: BookRepository = mockk()
    private lateinit var getBooksUseCase: GetBooksUseCase

    private val testBooks = listOf(
        Book(id = 1, title = "Kotlin Basics", lastOpened = 1000L, fileSize = 2048L, progress = 50),
        Book(id = 2, title = "Android Advanced", lastOpened = 3000L, fileSize = 1024L, progress = 10),
        Book(id = 3, title = "Jetpack Compose Guide", lastOpened = 2000L, fileSize = 4096L, progress = 90)
    )

    @Before
    fun setUp() {
        getBooksUseCase = GetBooksUseCase(bookRepository)
        every { bookRepository.getAllBooks() } returns flowOf(testBooks)
    }

    @Test
    fun `invoke with RECENT sort mode returns books sorted by lastOpened descending`() = runTest {
        val result = getBooksUseCase(LibrarySortMode.RECENT).first()

        assertEquals(3, result.size)
        assertEquals(2L, result[0].id) // lastOpened = 3000
        assertEquals(3L, result[1].id) // lastOpened = 2000
        assertEquals(1L, result[2].id) // lastOpened = 1000

        verify(exactly = 1) { bookRepository.getAllBooks() }
    }

    @Test
    fun `invoke with NAME sort mode returns books sorted by title case-insensitively ascending`() = runTest {
        val result = getBooksUseCase(LibrarySortMode.NAME).first()

        assertEquals(3, result.size)
        assertEquals(2L, result[0].id) // Android Advanced
        assertEquals(3L, result[1].id) // Jetpack Compose Guide
        assertEquals(1L, result[2].id) // Kotlin Basics
    }

    @Test
    fun `invoke with SIZE sort mode returns books sorted by fileSize descending`() = runTest {
        val result = getBooksUseCase(LibrarySortMode.SIZE).first()

        assertEquals(3, result.size)
        assertEquals(3L, result[0].id) // fileSize = 4096
        assertEquals(1L, result[1].id) // fileSize = 2048
        assertEquals(2L, result[2].id) // fileSize = 1024
    }

    @Test
    fun `invoke with PROGRESS sort mode returns books sorted by progress descending`() = runTest {
        val result = getBooksUseCase(LibrarySortMode.PROGRESS).first()

        assertEquals(3, result.size)
        assertEquals(3L, result[0].id) // progress = 90
        assertEquals(1L, result[1].id) // progress = 50
        assertEquals(2L, result[2].id) // progress = 10
    }
}
