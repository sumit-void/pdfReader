package com.example.pdfreader.presentation.screens.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pdfreader.data.preferences.UserPreferences
import com.example.pdfreader.domain.model.Book
import com.example.pdfreader.domain.model.LibrarySortMode
import com.example.pdfreader.domain.model.LibraryViewMode
import com.example.pdfreader.domain.usecase.DeleteBookUseCase
import com.example.pdfreader.domain.usecase.GetBooksUseCase
import com.example.pdfreader.domain.usecase.ImportPdfUseCase
import com.example.pdfreader.domain.usecase.SearchBooksUseCase
import com.example.pdfreader.domain.usecase.StreakInfo
import com.example.pdfreader.domain.usecase.StreakTrackerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class LibraryUiState(
    val books: List<Book> = emptyList(),
    val isLoading: Boolean = true,
    val isImporting: Boolean = false,
    val searchQuery: String = "",
    val sortMode: LibrarySortMode = LibrarySortMode.RECENT,
    val viewMode: LibraryViewMode = LibraryViewMode.GRID,
    val importError: String? = null,
    val importedBookId: Long? = null,
    val streakInfo: StreakInfo? = null
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val getBooksUseCase: GetBooksUseCase,
    private val searchBooksUseCase: SearchBooksUseCase,
    private val importPdfUseCase: ImportPdfUseCase,
    private val deleteBookUseCase: DeleteBookUseCase,
    private val userPreferences: UserPreferences,
    private val streakTrackerUseCase: StreakTrackerUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        loadPreferences()
        loadBooks()
        observeStreak()
    }

    private fun observeStreak() {
        viewModelScope.launch {
            streakTrackerUseCase().collectLatest { info ->
                _uiState.update { it.copy(streakInfo = info) }
            }
        }
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            userPreferences.libraryViewMode.collectLatest { mode ->
                _uiState.update { it.copy(viewMode = try { LibraryViewMode.valueOf(mode) } catch (_: Exception) { LibraryViewMode.GRID }) }
            }
        }
        viewModelScope.launch {
            userPreferences.librarySortMode.collectLatest { mode ->
                _uiState.update { it.copy(sortMode = try { LibrarySortMode.valueOf(mode) } catch (_: Exception) { LibrarySortMode.RECENT }) }
                loadBooks()
            }
        }
    }

    private fun loadBooks() {
        viewModelScope.launch {
            val query = _uiState.value.searchQuery
            val sortMode = _uiState.value.sortMode

            val booksFlow = if (query.isBlank()) {
                getBooksUseCase(sortMode)
            } else {
                searchBooksUseCase(query)
            }

            booksFlow.collectLatest { books ->
                _uiState.update {
                    it.copy(books = books, isLoading = false)
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadBooks()
    }

    fun setSortMode(mode: LibrarySortMode) {
        viewModelScope.launch {
            userPreferences.setLibrarySortMode(mode.name)
        }
        _uiState.update { it.copy(sortMode = mode) }
        loadBooks()
    }

    fun toggleViewMode() {
        val newMode = if (_uiState.value.viewMode == LibraryViewMode.GRID)
            LibraryViewMode.LIST else LibraryViewMode.GRID
        viewModelScope.launch {
            userPreferences.setLibraryViewMode(newMode.name)
        }
        _uiState.update { it.copy(viewMode = newMode) }
    }

    fun importPdf(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true) }
            try {
                val result = importPdfUseCase(uri)
                result.fold(
                    onSuccess = { book ->
                        _uiState.update {
                            it.copy(isImporting = false, importedBookId = book.id)
                        }
                    },
                    onFailure = { error ->
                        Timber.e(error, "Import failed")
                        _uiState.update {
                            it.copy(
                                isImporting = false,
                                importError = error.message ?: "Failed to import PDF"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Import failed")
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        importError = e.message ?: "Failed to import PDF"
                    )
                }
            }
        }
    }

    fun deleteBook(book: Book) {
        viewModelScope.launch {
            try {
                deleteBookUseCase(book)
            } catch (e: Exception) {
                Timber.e(e, "Delete failed")
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(importError = null) }
    }

    fun clearImportedBookId() {
        _uiState.update { it.copy(importedBookId = null) }
    }
}
