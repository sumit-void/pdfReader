package com.example.pdfreader.domain.model

data class TocEntry(
    val title: String,
    val pageNumber: Int,
    val level: Int = 0,
    val children: List<TocEntry> = emptyList()
)
