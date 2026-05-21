package com.example.pdfreader.presentation.screens.highlights

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pdfreader.domain.model.Highlight
import com.example.pdfreader.domain.repository.BookRepository
import com.example.pdfreader.domain.repository.HighlightRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class QuoteCardUiState(
    val highlight: Highlight? = null,
    val bookTitle: String = "",
    val bookAuthor: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class QuoteCardEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val highlightRepository: HighlightRepository,
    private val bookRepository: BookRepository
) : ViewModel() {

    private val highlightId: Long = savedStateHandle.get<Long>("highlightId") ?: -1L

    private val _uiState = MutableStateFlow(QuoteCardUiState())
    val uiState: StateFlow<QuoteCardUiState> = _uiState.asStateFlow()

    init {
        loadHighlightAndBookDetails()
    }

    private fun loadHighlightAndBookDetails() {
        if (highlightId == -1L) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "Invalid highlight selection"
                )
            }
            return
        }

        viewModelScope.launch {
            try {
                val highlight = highlightRepository.getHighlightById(highlightId)
                if (highlight != null) {
                    val book = bookRepository.getBookById(highlight.bookId)
                    _uiState.update {
                        it.copy(
                            highlight = highlight,
                            bookTitle = book?.title ?: "Unknown Book",
                            bookAuthor = book?.author ?: "Unknown Author",
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Highlight not found"
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading details for quote card editor")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load details"
                    )
                }
            }
        }
    }
}
