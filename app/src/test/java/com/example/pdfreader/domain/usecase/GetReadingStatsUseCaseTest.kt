package com.example.pdfreader.domain.usecase

import com.example.pdfreader.domain.model.ReadingStats
import com.example.pdfreader.domain.model.WeeklyActivity
import com.example.pdfreader.domain.repository.ReadingStatsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetReadingStatsUseCaseTest {

    private val readingStatsRepository: ReadingStatsRepository = mockk()
    private lateinit var getReadingStatsUseCase: GetReadingStatsUseCase

    @Before
    fun setUp() {
        getReadingStatsUseCase = GetReadingStatsUseCase(readingStatsRepository)
    }

    @Test
    fun `getStats returns correct stats from repository`() = runTest {
        val expectedStats = ReadingStats(
            booksRead = 5,
            pagesRead = 120,
            readingStreakDays = 3,
            totalReadingTimeMs = 3600000L
        )
        coEvery { readingStatsRepository.getReadingStats() } returns expectedStats

        val result = getReadingStatsUseCase.getStats()

        assertEquals(expectedStats, result)
        coVerify(exactly = 1) { readingStatsRepository.getReadingStats() }
    }

    @Test
    fun `getWeeklyActivity returns correct activity list from repository`() = runTest {
        val expectedActivity = listOf(
            WeeklyActivity(dayName = "Mon", readingTimeMinutes = 15, pagesRead = 10),
            WeeklyActivity(dayName = "Tue", readingTimeMinutes = 30, pagesRead = 20)
        )
        coEvery { readingStatsRepository.getWeeklyActivity() } returns expectedActivity

        val result = getReadingStatsUseCase.getWeeklyActivity()

        assertEquals(expectedActivity, result)
        coVerify(exactly = 1) { readingStatsRepository.getWeeklyActivity() }
    }
}
