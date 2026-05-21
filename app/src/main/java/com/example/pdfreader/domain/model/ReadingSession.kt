package com.example.pdfreader.domain.model

data class ReadingSession(
    val id: Long = 0,
    val bookId: Long = 0,
    val startTime: Long = 0,
    val endTime: Long = 0,
    val pagesRead: Int = 0
)
