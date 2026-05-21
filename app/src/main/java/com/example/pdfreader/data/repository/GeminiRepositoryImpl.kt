package com.example.pdfreader.data.repository

import com.example.pdfreader.BuildConfig
import com.example.pdfreader.domain.repository.GeminiRepository
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiRepositoryImpl @Inject constructor() : GeminiRepository {

    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

    override fun generateSummaryStream(text: String): Flow<String> = flow {
        if (text.isBlank()) {
            emit("No text was extracted from this page to summarize.")
            return@flow
        }
        
        try {
            val prompt = "Summarize the following text from a PDF book page in a clean, concise, and structured way. Focus on key ideas:\n\n$text"
            val responseStream = generativeModel.generateContentStream(prompt)
            responseStream.collect { chunk ->
                chunk.text?.let { emit(it) }
            }
        } catch (e: Exception) {
            emit("Error generating summary: ${e.localizedMessage ?: e.message}")
        }
    }
}
