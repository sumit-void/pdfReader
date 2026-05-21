package com.example.pdfreader.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey
    val id: Long = 1L, // Singleton row
    val dailyPagesGoal: Int = 10,
    val dailyTimeGoalMinutes: Int = 15
)
