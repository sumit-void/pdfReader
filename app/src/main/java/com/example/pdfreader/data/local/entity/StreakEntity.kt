package com.example.pdfreader.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "streaks")
data class StreakEntity(
    @PrimaryKey
    val date: String, // format: "yyyy-MM-dd"
    val pagesRead: Int = 0,
    val timeReadMs: Long = 0L
)
