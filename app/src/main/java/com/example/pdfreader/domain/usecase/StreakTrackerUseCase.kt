package com.example.pdfreader.domain.usecase

import com.example.pdfreader.data.local.dao.GoalDao
import com.example.pdfreader.data.local.dao.StreakDao
import com.example.pdfreader.data.local.entity.GoalEntity
import com.example.pdfreader.data.local.entity.StreakEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class StreakInfo(
    val currentStreak: Int,
    val pagesReadToday: Int,
    val timeReadMsToday: Long,
    val dailyPagesGoal: Int,
    val dailyTimeGoalMinutes: Int,
    val progress: Float
)

class StreakTrackerUseCase @Inject constructor(
    private val streakDao: StreakDao,
    private val goalDao: GoalDao
) {
    suspend fun getOrCreateGoal(): GoalEntity {
        val goal = goalDao.getGoal()
        if (goal == null) {
            val defaultGoal = GoalEntity()
            goalDao.insertGoal(defaultGoal)
            return defaultGoal
        }
        return goal
    }

    suspend fun getTodayStreakRecord(): StreakEntity {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return streakDao.getStreakByDate(today) ?: StreakEntity(today)
    }

    operator fun invoke(): Flow<StreakInfo> {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val yesterdayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
            Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000)
        )

        return combine(
            streakDao.getAllStreaks(),
            goalDao.observeGoal()
        ) { streaks, maybeGoal ->
            val goal = maybeGoal ?: GoalEntity()
            val todayRecord = streaks.find { it.date == todayStr } ?: StreakEntity(todayStr)
            val currentStreak = calculateStreak(streaks, todayStr, yesterdayStr)
            
            val pagesProgress = if (goal.dailyPagesGoal > 0) {
                todayRecord.pagesRead.toFloat() / goal.dailyPagesGoal
            } else {
                0f
            }

            StreakInfo(
                currentStreak = currentStreak,
                pagesReadToday = todayRecord.pagesRead,
                timeReadMsToday = todayRecord.timeReadMs,
                dailyPagesGoal = goal.dailyPagesGoal,
                dailyTimeGoalMinutes = goal.dailyTimeGoalMinutes,
                progress = pagesProgress.coerceIn(0f, 1f)
            )
        }
    }

    private fun calculateStreak(streaks: List<StreakEntity>, todayStr: String, yesterdayStr: String): Int {
        if (streaks.isEmpty()) return 0
        
        val readToday = streaks.any { it.date == todayStr && it.pagesRead > 0 }
        val readYesterday = streaks.any { it.date == yesterdayStr && it.pagesRead > 0 }
        
        if (!readToday && !readYesterday) {
            return 0
        }

        var streakCount = 0
        val checkDate = Calendar.getInstance().apply {
            if (!readToday && readYesterday) {
                add(Calendar.DAY_OF_YEAR, -1)
            }
        }
        
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        while (true) {
            val dateStr = sdf.format(checkDate.time)
            val dayRecord = streaks.find { it.date == dateStr }
            if (dayRecord != null && dayRecord.pagesRead > 0) {
                streakCount++
                checkDate.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }
        return streakCount
    }
}
