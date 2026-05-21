package com.example.pdfreader.presentation.screens.toc

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pdfreader.domain.model.TocEntry
import com.example.pdfreader.domain.repository.BookRepository
import com.example.pdfreader.domain.repository.GeminiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import timber.log.Timber
import java.io.File
import javax.inject.Inject

data class TocUiState(
    val bookId: Long = -1L,
    val tocEntries: List<TocEntry> = emptyList(),
    val totalPages: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class TocViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bookRepository: BookRepository,
    private val geminiRepository: GeminiRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val bookId: Long = savedStateHandle.get<Long>("bookId") ?: -1L

    private val _uiState = MutableStateFlow(TocUiState(bookId = bookId))
    val uiState: StateFlow<TocUiState> = _uiState.asStateFlow()

    init {
        loadToc()
    }

    private fun loadToc() {
        viewModelScope.launch {
            try {
                val book = bookRepository.getBookById(bookId)
                if (book == null) {
                    _uiState.update { it.copy(isLoading = false) }
                    return@launch
                }

                _uiState.update {
                    it.copy(
                        totalPages = book.pageCount,
                        isLoading = true
                    )
                }

                val cacheFile = File(context.filesDir, "ai_toc_${bookId}.json")
                var entries = emptyList<TocEntry>()

                if (cacheFile.exists()) {
                    try {
                        val jsonStr = cacheFile.readText()
                        entries = parseJsonToTocEntries(jsonStr)
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to read or parse cached AI TOC JSON")
                    }
                }

                if (entries.isEmpty()) {
                    // Extract text from page 0 to 5
                    val extractedText = StringBuilder()
                    val pagesToExtract = 5.coerceAtMost(book.pageCount - 1)
                    for (i in 0..pagesToExtract) {
                        try {
                            val text = bookRepository.extractPageText(book.filePath, i)
                            if (text.isNotBlank()) {
                                extractedText.append(text).append("\n")
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to extract text for page $i")
                        }
                    }

                    val tocText = extractedText.toString()
                    if (tocText.isNotBlank()) {
                        try {
                            var response = geminiRepository.generateTableOfContents(tocText)
                            
                            // Strip markdown code fences if present in the response
                            if (response.contains("```json")) {
                                response = response.substringAfter("```json").substringBefore("```")
                            } else if (response.contains("```")) {
                                response = response.substringAfter("```").substringBefore("```")
                            }
                            response = response.trim()
                            
                            entries = parseJsonToTocEntries(response)
                            if (entries.isNotEmpty()) {
                                cacheFile.writeText(response)
                            }
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to generate AI TOC")
                        }
                    }
                }

                _uiState.update {
                    it.copy(
                        tocEntries = entries,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load TOC")
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun parseJsonToTocEntries(jsonStr: String): List<TocEntry> {
        val entries = mutableListOf<TocEntry>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val title = jsonObject.optString("title", "Chapter ${i + 1}")
                // Gemini is 1-indexed for page numbers, so subtract 1 to get 0-indexed page for TocEntry
                val pageNum = (jsonObject.optInt("pageNumber", 1) - 1).coerceAtLeast(0)
                entries.add(TocEntry(title = title, pageNumber = pageNum))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse JSON string: $jsonStr")
        }
        return entries
    }
}
