package com.example.pdfreader.viewmodel

import android.app.Application
import android.text.TextPaint
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.pdfreader.data.Book
import com.example.pdfreader.data.BookRepository
import com.example.pdfreader.data.Bookmark
import com.example.pdfreader.data.Chapter
import com.example.pdfreader.data.PaginationEngine
import com.example.pdfreader.data.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReaderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BookRepository(application)

    private val _book = MutableLiveData<Book?>()
    val book: LiveData<Book?> = _book

    // Observe bookmarks for the loaded book
    val bookmarks: LiveData<List<Bookmark>> = _book.switchMap { book ->
        if (book != null) {
            repository.getBookmarksForBook(book.id)
        } else {
            MutableLiveData(emptyList())
        }
    }

    private val _chapters = MutableLiveData<List<Chapter>>(emptyList())
    val chapters: LiveData<List<Chapter>> = _chapters

    private val _searchResults = MutableLiveData<List<SearchResult>>(emptyList())
    val searchResults: LiveData<List<SearchResult>> = _searchResults

    private val _isSearching = MutableLiveData<Boolean>(false)
    val isSearching: LiveData<Boolean> = _isSearching

    // UI reading preference states
    val theme = MutableLiveData<String>("sepia") // sepia, dark, light
    val fontFamily = MutableLiveData<String>("lora") // lora, playfair, sans
    val fontSize = MutableLiveData<Float>(18f) // in sp
    val marginType = MutableLiveData<String>("standard") // narrow, standard, wide
    val lineSpacing = MutableLiveData<Float>(1.2f)
    val brightness = MutableLiveData<Float>(0.8f) // 0.0 to 1.0

    // Paginated pages data
    private val _paginatedPages = MutableLiveData<List<String>>(emptyList())
    val paginatedPages: LiveData<List<String>> = _paginatedPages

    private val _isPaginating = MutableLiveData<Boolean>(false)
    val isPaginating: LiveData<Boolean> = _isPaginating

    // Current page mapping (paginated page index)
    private val _currentPage = MutableLiveData<Int>(0)
    val currentPage: LiveData<Int> = _currentPage

    // Dimensions of view port
    private var viewportWidth = 0
    private var viewportHeight = 0
    private var textPaint: TextPaint? = null
    
    // Extracted raw text cache
    private var rawTextCache: String = ""

    fun loadBook(bookId: Long) {
        viewModelScope.launch {
            val loadedBook = repository.getBookById(bookId) ?: return@launch
            _book.value = loadedBook
            _currentPage.value = loadedBook.lastPageRead

            // Load outline TOC
            val toc = repository.getTableOfContents(loadedBook.localFilePath)
            _chapters.value = toc

            // Load raw text for reflow pagination in background
            _isPaginating.value = true
            rawTextCache = repository.loadFullText(loadedBook.localFilePath)
            if (rawTextCache.isBlank()) {
                // If text extraction fails, load placeholder text
                rawTextCache = "Failed to extract readable text content from this PDF.\nPaperback is designed to reflow text, but this file may be a scanned image-only PDF.\n\nPlease import a text-based PDF to experience reflowed paperback formatting."
            }
            triggerPagination()
        }
    }

    fun setViewportSize(width: Int, height: Int, paint: TextPaint) {
        if (viewportWidth == width && viewportHeight == height && textPaint == paint) return
        viewportWidth = width
        viewportHeight = height
        textPaint = paint
        triggerPagination()
    }

    fun updateReadingPreferences() {
        triggerPagination()
    }

    private fun triggerPagination() {
        val paint = textPaint ?: return
        val w = viewportWidth
        val h = viewportHeight
        val text = rawTextCache
        if (w <= 0 || h <= 0 || text.isEmpty()) return

        _isPaginating.value = true
        viewModelScope.launch(Dispatchers.Default) {
            val spacing = lineSpacing.value ?: 1.2f
            
            // Adjust paint text size based on settings
            val size = fontSize.value ?: 18f
            // We temporarily adjust paint size for measurement (fragments will use their own paint)
            val measuredPaint = TextPaint(paint).apply {
                textSize = size * (paint.density ?: 1f)
            }

            val marginWidth = getMarginSize(marginType.value ?: "standard", w)
            val availableWidth = w - (marginWidth * 2)
            val availableHeight = h - 60 // deduct headers/footers spacing

            val engine = PaginationEngine(measuredPaint, availableHeight, availableWidth, spacing)
            val pages = engine.paginate(text)

            withContext(Dispatchers.Main) {
                _paginatedPages.value = pages
                _isPaginating.value = false
                
                // Adjust current page if out of bounds
                val curr = _currentPage.value ?: 0
                if (curr >= pages.size) {
                    _currentPage.value = Math.max(0, pages.size - 1)
                }
            }
        }
    }

    private fun getMarginSize(type: String, viewportWidth: Int): Int {
        return when (type) {
            "narrow" -> (viewportWidth * 0.05).toInt()
            "wide" -> (viewportWidth * 0.12).toInt()
            else -> (viewportWidth * 0.08).toInt() // standard
        }
    }

    fun setCurrentPage(page: Int) {
        if (_currentPage.value == page) return
        _currentPage.value = page
        
        // Persist last read page to DB
        viewModelScope.launch {
            _book.value?.let { currentBook ->
                val updated = currentBook.copy(lastPageRead = page)
                repository.updateBook(updated)
            }
        }
    }

    fun toggleBookmark(pageNumber: Int) {
        val currentBook = _book.value ?: return
        viewModelScope.launch {
            val isCurrentlyBookmarked = repository.isBookmarked(currentBook.id, pageNumber)
            if (isCurrentlyBookmarked) {
                repository.removeBookmark(currentBook.id, pageNumber)
            } else {
                repository.addBookmark(currentBook.id, pageNumber)
            }
        }
    }

    fun performSearch(query: String) {
        val currentBook = _book.value ?: return
        _isSearching.value = true
        viewModelScope.launch {
            val results = repository.searchBook(currentBook.localFilePath, query)
            _searchResults.value = results
            _isSearching.value = false
        }
    }

    fun clearSearch() {
        _searchResults.value = emptyList()
    }
}
// Helper to get display density if needed
private val TextPaint.density: Float?
    get() = 2f // Default mock scale, actual fragment will set standard values
