package com.example.pdfreader.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class Book(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val author: String?,
    val localFilePath: String, // Path in app internal storage (filesDir/books/...)
    val coverPath: String,     // Path to cover thumbnail (filesDir/covers/...)
    val lastPageRead: Int = 0, // Maps to the paginated index
    val totalPages: Int = 0,   // Total pages of PDF source
    val addedDate: Long = System.currentTimeMillis()
)
