package com.example.pdfreader.domain.model

data class Highlight(
    val id: Long = 0,
    val bookId: Long = 0,
    val pageNumber: Int = 0,
    val startIndex: Int = 0,
    val endIndex: Int = 0,
    val text: String = "",
    val color: HighlightColor = HighlightColor.YELLOW,
    val dateCreated: Long = 0
)

enum class HighlightColor {
    YELLOW, GREEN, PINK
}
