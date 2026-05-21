package com.example.pdfreader.domain.repository

import kotlinx.coroutines.flow.Flow

interface GeminiRepository {
    fun generateSummaryStream(text: String): Flow<String>
    suspend fun generateTableOfContents(tocText: String): String
}
