package com.example.pdfreader.data.repository

import com.example.pdfreader.data.local.dao.BookDao
import com.example.pdfreader.data.local.dao.ReadingSessionDao
import com.example.pdfreader.data.local.dao.StreakDao
import com.example.pdfreader.data.local.entity.ReadingSessionEntity
import com.example.pdfreader.data.local.entity.StreakEntity
import com.example.pdfreader.domain.model.ReadingSession
import com.example.pdfreader.domain.model.ReadingStats
import com.example.pdfreader.domain.model.WeeklyActivity
import com.example.pdfreader.domain.repository.ReadingStatsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReadingStatsRepositoryImpl @Inject constructor(
    private val sessionDao: ReadingSessionDao,
    private val bookDao: BookDao,
    private val streakDao: StreakDao
) : ReadingStatsRepository {

    override suspend fun startSession(bookId: Long): Long {
        return sessionDao.insertSession(
            ReadingSessionEntity(bookId = bookId, startTime = System.currentTimeMillis())
        )
    }

    override suspend fun endSession(sessionId: Long, pagesRead: Int) {
        val session = sessionDao.getSessionById(sessionId)
        if (session != null) {
            val endTime = System.currentTimeMillis()
            sessionDao.updateSession(
                session.copy(
                    endTime = endTime,
                    pagesRead = pagesRead
                )
            )

            // Update daily streak stats
            val durationMs = endTime - session.startTime
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val existingStreak = streakDao.getStreakByDate(todayStr)
            if (existingStreak != null) {
                streakDao.updateStreak(
                    existingStreak.copy(
                        pagesRead = existingStreak.pagesRead + pagesRead,
                        timeReadMs = existingStreak.timeReadMs + durationMs
                    )
                )
            } else {
                streakDao.insertStreak(
                    StreakEntity(
                        date = todayStr,
                        pagesRead = pagesRead,
                        timeReadMs = durationMs
                    )
                )
            }
        }
    }

    override suspend fun getReadingStats(): ReadingStats {
        val booksRead = bookDao.getBookCount()
        val totalPagesRead = sessionDao.getTotalPagesRead() ?: 0
        val totalReadingTimeMs = sessionDao.getTotalReadingTime() ?: 0

        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -30)
        }
        val readingStreak = sessionDao.getReadingDaysSince(calendar.timeInMillis)

        return ReadingStats(
            booksRead = booksRead,
            pagesRead = totalPagesRead,
            readingStreakDays = readingStreak,
            totalReadingTimeMs = totalReadingTimeMs
        )
    }

    override suspend fun getWeeklyActivity(): List<WeeklyActivity> {
        val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val activities = mutableListOf<WeeklyActivity>()

        for (i in 0 until 7) {
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -(6 - i))
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            val dayIndex = when (dayOfWeek) {
                Calendar.MONDAY -> 0
                Calendar.TUESDAY -> 1
                Calendar.WEDNESDAY -> 2
                Calendar.THURSDAY -> 3
                Calendar.FRIDAY -> 4
                Calendar.SATURDAY -> 5
                Calendar.SUNDAY -> 6
                else -> 0
            }

            activities.add(
                WeeklyActivity(
                    dayName = dayNames[dayIndex],
                    readingTimeMinutes = 0,
                    pagesRead = 0
                )
            )
        }

        return activities
    }

    override suspend fun clearHistory() {
        sessionDao.deleteAllSessions()
    }

    override fun getAllSessions(): Flow<List<ReadingSession>> {
        return sessionDao.getAllSessions().map { entities ->
            entities.map {
                ReadingSession(
                    id = it.id,
                    bookId = it.bookId,
                    startTime = it.startTime,
                    endTime = it.endTime,
                    pagesRead = it.pagesRead
                )
            }
        }
    }
}
