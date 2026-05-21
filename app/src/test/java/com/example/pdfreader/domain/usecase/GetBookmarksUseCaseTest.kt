package com.example.pdfreader.domain.usecase

import com.example.pdfreader.domain.model.Bookmark
import com.example.pdfreader.domain.repository.BookmarkRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetBookmarksUseCaseTest {

    private val bookmarkRepository: BookmarkRepository = mockk()
    private lateinit var getBookmarksUseCase: GetBookmarksUseCase

    @Before
    fun setUp() {
        getBookmarksUseCase = GetBookmarksUseCase(bookmarkRepository)
    }

    @Test
    fun `invoke with bookId returns flow of bookmarks from repository`() = runTest {
        val bookId = 1L
        val expectedBookmarks = listOf(
            Bookmark(id = 1, bookId = bookId, pageNumber = 5, note = "Interesting page"),
            Bookmark(id = 2, bookId = bookId, pageNumber = 12, note = "Chapter start")
        )
        every { bookmarkRepository.getBookmarksForBook(bookId) } returns flowOf(expectedBookmarks)

        val result = getBookmarksUseCase(bookId).first()

        assertEquals(expectedBookmarks, result)
        verify(exactly = 1) { bookmarkRepository.getBookmarksForBook(bookId) }
    }
}
