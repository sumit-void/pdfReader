package com.example.pdfreader.domain.model

data class ReadingStats(
    val booksRead: Int = 0,
    val pagesRead: Int = 0,
    val readingStreakDays: Int = 0,
    val totalReadingTimeMs: Long = 0
)

data class WeeklyActivity(
    val dayName: String = "",
    val readingTimeMinutes: Int = 0,
    val pagesRead: Int = 0
)
