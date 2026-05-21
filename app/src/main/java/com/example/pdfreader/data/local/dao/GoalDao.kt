package com.example.pdfreader.data.local.dao

import androidx.room.*
import com.example.pdfreader.data.local.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {

    @Query("SELECT * FROM goals WHERE id = 1 LIMIT 1")
    suspend fun getGoal(): GoalEntity?

    @Query("SELECT * FROM goals WHERE id = 1 LIMIT 1")
    fun observeGoal(): Flow<GoalEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity)

    @Update
    suspend fun updateGoal(goal: GoalEntity)
}
