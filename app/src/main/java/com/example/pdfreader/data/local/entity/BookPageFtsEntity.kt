package com.example.pdfreader.data.local.entity

import androidx.room.Entity
import androidx.room.Fts4

@Fts4
@Entity(tableName = "book_pages_fts")
data class BookPageFtsEntity(
    val pageText: String
)
