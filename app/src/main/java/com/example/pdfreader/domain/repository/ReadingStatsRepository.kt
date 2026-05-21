package com.example.pdfreader.domain.repository

import com.example.pdfreader.domain.model.ReadingSession
import com.example.pdfreader.domain.model.ReadingStats
import com.example.pdfreader.domain.model.WeeklyActivity
import kotlinx.coroutines.flow.Flow

interface ReadingStatsRepository {
    suspend fun startSession(bookId: Long): Long
    suspend fun endSession(sessionId: Long, pagesRead: Int)
    suspend fun getReadingStats(): ReadingStats
    suspend fun getWeeklyActivity(): List<WeeklyActivity>
    suspend fun clearHistory()
    fun getAllSessions(): Flow<List<ReadingSession>>
}
