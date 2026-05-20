package com.example.pdfreader.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.pdfreader.data.Book
import com.example.pdfreader.data.BookRepository
import kotlinx.coroutines.launch

sealed class ImportState {
    object Idle : ImportState()
    object Loading : ImportState()
    data class Success(val book: Book) : ImportState()
    data class Error(val message: String) : ImportState()
}

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = BookRepository(application)
    val books: LiveData<List<Book>> = repository.allBooks
    
    private val _importState = MutableLiveData<ImportState>(ImportState.Idle)
    val importState: LiveData<ImportState> = _importState
    
    fun importPdf(uri: Uri, displayName: String) {
        _importState.value = ImportState.Loading
        viewModelScope.launch {
            try {
                val book = repository.importBook(uri, displayName)
                _importState.value = ImportState.Success(book)
            } catch (e: Exception) {
                _importState.value = ImportState.Error(e.message ?: "Unknown error during import")
            }
        }
    }
    
    fun resetImportState() {
        _importState.value = ImportState.Idle
    }
    
    fun deleteBook(book: Book) {
        viewModelScope.launch {
            repository.deleteBook(book)
        }
    }
}
