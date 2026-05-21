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
            modelName = "generative-classifier", // Wait, let's keep the model name "gemini-1.5-flash" to avoid compilation/runtime model errors!
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

    // Wait, let's make sure modelName = "gemini-1.5-flash" as was originally written!
    private val model by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

    override fun generateSummaryStream(text: String): Flow<String> = flow {
        if (text.isBlank()) {
            throw IllegalArgumentException("No text extracted from this page to summarize.")
        }
        
        val prompt = "Summarize the following text from a PDF book page in a clean, concise, and structured way. Focus on key ideas:\n\n$text"
        val responseStream = model.generateContentStream(prompt)
        responseStream.collect { chunk ->
            chunk.text?.let { emit(it) }
        }
    }

    override suspend fun generateTableOfContents(tocText: String): String {
        if (tocText.isBlank()) {
            throw IllegalArgumentException("Text extracted from pages 0-5 is blank.")
        }
        val prompt = """
            You are a professional PDF parsing assistant. 
            Analyze the following text extracted from the beginning of a PDF book (pages 0 to 5) and identify the book's Table of Contents or chapter divisions.
            
            Return ONLY a raw JSON array of objects where each object has two keys: "title" (the chapter name, String) and "pageNumber" (the starting page number, 1-indexed, Integer).
            Do NOT wrap the JSON in Markdown code blocks (e.g. do not use ```json ... ```) or include any explanation. If no Table of Contents is found, try to identify the major starting sections or chapter titles with their page numbers.
            
            Here is the text to analyze:
            
            $tocText
        """.trimIndent()
        
        val response = model.generateContent(prompt)
        return response.text ?: "[]"
    }
}
