package com.example.pdfreader.domain.usecase

import com.example.pdfreader.domain.model.ReadingStats
import com.example.pdfreader.domain.model.WeeklyActivity
import com.example.pdfreader.domain.repository.ReadingStatsRepository
import javax.inject.Inject

class GetReadingStatsUseCase @Inject constructor(
    private val readingStatsRepository: ReadingStatsRepository
) {
    suspend fun getStats(): ReadingStats {
        return readingStatsRepository.getReadingStats()
    }

    suspend fun getWeeklyActivity(): List<WeeklyActivity> {
        return readingStatsRepository.getWeeklyActivity()
    }
}
