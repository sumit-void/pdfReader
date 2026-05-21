package com.example.pdfreader.domain.repository

import com.example.pdfreader.domain.model.Highlight
import kotlinx.coroutines.flow.Flow

interface HighlightRepository {
    fun getHighlightsForBook(bookId: Long): Flow<List<Highlight>>
    fun getHighlightsForPage(bookId: Long, page: Int): Flow<List<Highlight>>
    suspend fun addHighlight(highlight: Highlight): Long
    suspend fun deleteHighlight(id: Long)
    suspend fun getHighlightById(id: Long): Highlight?
}
