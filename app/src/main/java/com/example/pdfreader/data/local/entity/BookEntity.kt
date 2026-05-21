package com.example.pdfreader.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val filePath: String,
    val title: String,
    val author: String = "Unknown Author",
    val pageCount: Int = 0,
    val coverPath: String = "",
    val currentPage: Int = 0,
    val totalReadTimeMs: Long = 0,
    val fileSize: Long = 0,
    val dateAdded: Long = System.currentTimeMillis(),
    val lastOpened: Long = 0
)
