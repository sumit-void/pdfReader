package com.example.pdfreader.domain.usecase

import com.example.pdfreader.domain.repository.BookmarkRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ToggleBookmarkUseCaseTest {

    private val bookmarkRepository: BookmarkRepository = mockk()
    private lateinit var toggleBookmarkUseCase: ToggleBookmarkUseCase

    @Before
    fun setUp() {
        toggleBookmarkUseCase = ToggleBookmarkUseCase(bookmarkRepository)
    }

    @Test
    fun `invoke to add bookmark returns true when successful`() = runTest {
        val bookId = 1L
        val page = 5
        coEvery { bookmarkRepository.toggleBookmark(bookId, page) } returns true

        val result = toggleBookmarkUseCase(bookId, page)

        assertTrue(result)
        coVerify(exactly = 1) { bookmarkRepository.toggleBookmark(bookId, page) }
    }

    @Test
    fun `invoke to remove bookmark returns false when successful`() = runTest {
        val bookId = 1L
        val page = 5
        coEvery { bookmarkRepository.toggleBookmark(bookId, page) } returns false

        val result = toggleBookmarkUseCase(bookId, page)

        assertFalse(result)
        coVerify(exactly = 1) { bookmarkRepository.toggleBookmark(bookId, page) }
    }
}
