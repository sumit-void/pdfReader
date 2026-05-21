package com.example.pdfreader.domain.usecase

import com.example.pdfreader.domain.model.Highlight
import com.example.pdfreader.domain.model.HighlightColor
import com.example.pdfreader.domain.repository.HighlightRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetHighlightsUseCaseTest {

    private val highlightRepository: HighlightRepository = mockk()
    private lateinit var getHighlightsUseCase: GetHighlightsUseCase

    @Before
    fun setUp() {
        getHighlightsUseCase = GetHighlightsUseCase(highlightRepository)
    }

    @Test
    fun `invoke with bookId returns flow of highlights from repository`() = runTest {
        val bookId = 1L
        val expectedHighlights = listOf(
            Highlight(id = 1, bookId = bookId, pageNumber = 3, text = "First highlight", color = HighlightColor.YELLOW),
            Highlight(id = 2, bookId = bookId, pageNumber = 7, text = "Second highlight", color = HighlightColor.GREEN)
        )
        every { highlightRepository.getHighlightsForBook(bookId) } returns flowOf(expectedHighlights)

        val result = getHighlightsUseCase(bookId).first()

        assertEquals(expectedHighlights, result)
        verify(exactly = 1) { highlightRepository.getHighlightsForBook(bookId) }
    }
}
