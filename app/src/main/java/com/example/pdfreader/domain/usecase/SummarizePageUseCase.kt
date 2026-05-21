package com.example.pdfreader.domain.usecase

import com.example.pdfreader.domain.repository.BookRepository
import com.example.pdfreader.domain.repository.GeminiRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SummarizePageUseCase @Inject constructor(
    private val bookRepository: BookRepository,
    private val geminiRepository: GeminiRepository
) {
    suspend operator fun invoke(filePath: String, pageIndex: Int): Flow<String> {
        val extractedText = bookRepository.extractPageText(filePath, pageIndex)
        return geminiRepository.generateSummaryStream(extractedText)
    }
}
