package com.example.pdfreader.presentation.screens.highlights

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pdfreader.domain.model.Highlight
import com.example.pdfreader.domain.repository.HighlightRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HighlightsUiState(
    val bookId: Long = -1L,
    val highlights: List<Highlight> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class HighlightsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val highlightRepository: HighlightRepository
) : ViewModel() {

    private val bookId: Long = savedStateHandle.get<Long>("bookId") ?: -1L

    private val _uiState = MutableStateFlow(HighlightsUiState(bookId = bookId))
    val uiState: StateFlow<HighlightsUiState> = _uiState.asStateFlow()

    init {
        loadHighlights()
    }

    private fun loadHighlights() {
        viewModelScope.launch {
            highlightRepository.getHighlightsForBook(bookId).collectLatest { highlights ->
                _uiState.update { it.copy(highlights = highlights, isLoading = false) }
            }
        }
    }

    fun deleteHighlight(highlightId: Long) {
        viewModelScope.launch {
            highlightRepository.deleteHighlight(highlightId)
        }
    }
}
