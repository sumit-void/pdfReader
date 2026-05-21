package com.example.pdfreader.domain.model

data class Bookmark(
    val id: Long = 0,
    val bookId: Long = 0,
    val pageNumber: Int = 0,
    val note: String = "",
    val dateCreated: Long = 0
)
