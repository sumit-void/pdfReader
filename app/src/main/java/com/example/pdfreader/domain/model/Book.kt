package com.example.pdfreader.domain.model

data class Book(
    val id: Long = 0,
    val filePath: String = "",
    val title: String = "",
    val author: String = "Unknown Author",
    val pageCount: Int = 0,
    val coverPath: String = "",
    val currentPage: Int = 0,
    val totalReadTimeMs: Long = 0,
    val fileSize: Long = 0,
    val dateAdded: Long = 0,
    val lastOpened: Long = 0,
    val progress: Int = 0
)
