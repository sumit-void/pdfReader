package com.example.pdfreader.data.local.dao

import androidx.room.*
import com.example.pdfreader.data.local.entity.StreakEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StreakDao {

    @Query("SELECT * FROM streaks WHERE date = :date LIMIT 1")
    suspend fun getStreakByDate(date: String): StreakEntity?

    @Query("SELECT * FROM streaks WHERE date = :date LIMIT 1")
    fun observeStreakByDate(date: String): Flow<StreakEntity?>

    @Query("SELECT * FROM streaks ORDER BY date DESC")
    fun getAllStreaks(): Flow<List<StreakEntity>>

    @Query("SELECT * FROM streaks ORDER BY date DESC")
    suspend fun getAllStreaksVal(): List<StreakEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreak(streak: StreakEntity)

    @Update
    suspend fun updateStreak(streak: StreakEntity)

    @Query("DELETE FROM streaks")
    suspend fun clearAllStreaks()
}
