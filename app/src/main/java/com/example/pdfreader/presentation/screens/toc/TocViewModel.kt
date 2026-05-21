package com.example.pdfreader.presentation.screens.toc

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pdfreader.domain.model.TocEntry
import com.example.pdfreader.domain.repository.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
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
    private val bookRepository: BookRepository
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
                if (book != null) {
                    _uiState.update {
                        it.copy(
                            totalPages = book.pageCount,
                            isLoading = false
                        )
                    }
                    // TOC parsing would require PDFBox to extract the document outline
                    // For now, we provide a fallback page list
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load TOC")
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
