package com.example.pdfreader.presentation.screens.reader

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pdfreader.data.pdf.PdfRenderCache
import com.example.pdfreader.data.preferences.UserPreferences
import com.example.pdfreader.domain.model.AppTheme
import com.example.pdfreader.domain.model.Book
import com.example.pdfreader.domain.model.PageTurnStyle
import com.example.pdfreader.domain.repository.BookRepository
import com.example.pdfreader.domain.repository.BookmarkRepository
import com.example.pdfreader.domain.repository.ReadingStatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.example.pdfreader.data.service.TtsService
import com.example.pdfreader.domain.usecase.SummarizePageUseCase
import dagger.hilt.android.qualifiers.ApplicationContext

sealed class ReaderUiState {
    data object Loading : ReaderUiState()
    data class Error(val message: String) : ReaderUiState()
    data class Success(
        val book: Book,
        val currentPage: Int = 0,
        val totalPages: Int = 0,
        val currentBitmap: Bitmap? = null,
        val isBookmarked: Boolean = false,
        val showControls: Boolean = false,
        val theme: AppTheme = AppTheme.LIGHT,
        val pageTurnStyle: PageTurnStyle = PageTurnStyle.CURL,
        val brightness: Float = -1f,
        val keepScreenAwake: Boolean = false,
        val isVerticalScroll: Boolean = false,
        val isSummarizing: Boolean = false,
        val summaryText: String = "",
        val summaryError: String? = null,
        val isTtsActive: Boolean = false,
        val isTtsPlaying: Boolean = false,
        val ttsWordRange: Pair<Int, Int>? = null,
        val pageText: String = "",
        val zoomLevel: Float = 1f
    ) : ReaderUiState()
}

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val bookRepository: BookRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val readingStatsRepository: ReadingStatsRepository,
    private val pdfRenderCache: PdfRenderCache,
    private val userPreferences: UserPreferences,
    private val summarizePageUseCase: SummarizePageUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private var ttsService: TtsService? = null
    private var isTtsBound = false
    private var pendingTextToSpeak: String? = null

    private val ttsConnection = object : android.content.ServiceConnection {
        override fun onServiceConnected(name: android.content.ComponentName?, service: IBinder?) {
            val binder = service as? TtsService.TtsBinder
            ttsService = binder?.getService()
            isTtsBound = true
            
            viewModelScope.launch {
                ttsService?.isPlaying?.collectLatest { playing ->
                    updateSuccessState { it.copy(isTtsPlaying = playing) }
                }
            }
            viewModelScope.launch {
                ttsService?.currentWordRange?.collectLatest { range ->
                    updateSuccessState { it.copy(ttsWordRange = range) }
                }
            }

            pendingTextToSpeak?.let { text ->
                ttsService?.startSpeaking(text)
                pendingTextToSpeak = null
            }
        }

        override fun onServiceDisconnected(name: android.content.ComponentName?) {
            ttsService = null
            isTtsBound = false
            updateSuccessState { it.copy(isTtsPlaying = false, ttsWordRange = null) }
        }
    }

    private val bookId: Long = savedStateHandle.get<Long>("bookId") ?: -1L

    private val _uiState = MutableStateFlow<ReaderUiState>(ReaderUiState.Loading)
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private var sessionId: Long = -1L
    private var startPage: Int = 0
    private var screenWidth: Int = 1080

    private val progressSaveFlow = MutableSharedFlow<Int>()
    private var lastSavedPage = 0

    init {
        loadBook()
        observePreferences()
        observeNavigationJumps()
        
        viewModelScope.launch {
            progressSaveFlow
                .debounce(500)
                .collectLatest { page ->
                    if (kotlin.math.abs(page - lastSavedPage) >= 3 || page == 0 || page == (uiState.value as? ReaderUiState.Success)?.totalPages?.minus(1)) {
                        bookRepository.updateReadingProgress(bookId, page)
                        lastSavedPage = page
                    }
                }
        }
    }

    fun setScreenWidth(width: Int) {
        screenWidth = width
    }

    private fun loadBook() {
        viewModelScope.launch {
            try {
                val book = bookRepository.getBookById(bookId)
                if (book == null) {
                    _uiState.value = ReaderUiState.Error("Book not found")
                    return@launch
                }

                val pageCount = withContext(Dispatchers.IO) {
                    pdfRenderCache.openPdf(book.filePath)
                }

                val currentPage = book.currentPage.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
                startPage = currentPage

                _uiState.value = ReaderUiState.Success(
                    book = book,
                    currentPage = currentPage,
                    totalPages = pageCount
                )

                // Start reading session
                sessionId = readingStatsRepository.startSession(bookId)

                // Update last opened
                userPreferences.setLastOpenedBookId(bookId)
                bookRepository.updateReadingProgress(bookId, currentPage)
                lastSavedPage = currentPage

                // Render current page
                renderPage(currentPage)

                // Observe bookmark status
                observeBookmark(currentPage)

            } catch (e: Exception) {
                Timber.e(e, "Failed to load book")
                _uiState.value = ReaderUiState.Error(
                    e.message ?: "Failed to open PDF"
                )
            }
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            userPreferences.theme.collectLatest { theme ->
                updateSuccessState { it.copy(theme = try { AppTheme.valueOf(theme) } catch (_: Exception) { AppTheme.LIGHT }) }
            }
        }
        viewModelScope.launch {
            userPreferences.pageTurnStyle.collectLatest { style ->
                updateSuccessState { it.copy(pageTurnStyle = try { PageTurnStyle.valueOf(style) } catch (_: Exception) { PageTurnStyle.CURL }) }
            }
        }
        viewModelScope.launch {
            userPreferences.brightness.collectLatest { brightness ->
                updateSuccessState { it.copy(brightness = brightness) }
            }
        }
        viewModelScope.launch {
            userPreferences.keepScreenAwake.collectLatest { awake ->
                updateSuccessState { it.copy(keepScreenAwake = awake) }
            }
        }
        viewModelScope.launch {
            userPreferences.getBookZoomLevel(bookId).collectLatest { zoom ->
                updateSuccessState { it.copy(zoomLevel = zoom) }
            }
        }
    }

    private fun observeNavigationJumps() {
        viewModelScope.launch {
            savedStateHandle.getStateFlow<Int?>("jumpToPage", null).collectLatest { page ->
                if (page != null) {
                    goToPage(page)
                    savedStateHandle.remove<Int>("jumpToPage")
                }
            }
        }
    }

    private fun observeBookmark(page: Int) {
        viewModelScope.launch {
            bookmarkRepository.isPageBookmarked(bookId, page).collectLatest { isBookmarked ->
                updateSuccessState { it.copy(isBookmarked = isBookmarked) }
            }
        }
    }

    fun goToPage(page: Int) {
        viewModelScope.launch {
            val state = _uiState.value as? ReaderUiState.Success ?: return@launch
            val validPage = page.coerceIn(0, (state.totalPages - 1).coerceAtLeast(0))

            if (state.isTtsActive) {
                stopTts()
            }

            updateSuccessState { 
                it.copy(
                    currentPage = validPage,
                    pageText = "",
                    ttsWordRange = null,
                    summaryText = "",
                    summaryError = null,
                    isSummarizing = false
                ) 
            }
            renderPage(validPage)
            observeBookmark(validPage)

            // Save progress via debounced flow
            progressSaveFlow.emit(validPage)
        }
    }

    fun nextPage() {
        val state = _uiState.value as? ReaderUiState.Success ?: return
        if (state.currentPage < state.totalPages - 1) {
            goToPage(state.currentPage + 1)
        }
    }

    fun previousPage() {
        val state = _uiState.value as? ReaderUiState.Success ?: return
        if (state.currentPage > 0) {
            goToPage(state.currentPage - 1)
        }
    }

    fun toggleControls() {
        updateSuccessState { it.copy(showControls = !it.showControls) }
    }

    fun toggleBookmark() {
        viewModelScope.launch {
            val state = _uiState.value as? ReaderUiState.Success ?: return@launch
            bookmarkRepository.toggleBookmark(bookId, state.currentPage)
        }
    }

    fun toggleScrollMode() {
        updateSuccessState { it.copy(isVerticalScroll = !it.isVerticalScroll) }
    }

    private suspend fun renderPage(pageIndex: Int) {
        try {
            val theme = (_uiState.value as? ReaderUiState.Success)?.theme ?: AppTheme.LIGHT
            val bitmap = pdfRenderCache.renderPage(pageIndex, screenWidth, theme)
            updateSuccessState { it.copy(currentBitmap = bitmap) }
        } catch (e: Exception) {
            Timber.e(e, "Failed to render page $pageIndex")
        }
    }

    fun renderPageForPager(pageIndex: Int, callback: (Bitmap?) -> Unit) {
        viewModelScope.launch {
            try {
                val theme = (_uiState.value as? ReaderUiState.Success)?.theme ?: AppTheme.LIGHT
                val bitmap = pdfRenderCache.renderPage(pageIndex, screenWidth, theme)
                callback(bitmap)
            } catch (e: Exception) {
                Timber.e(e, "Failed to render page $pageIndex for pager")
                callback(null)
            }
        }
    }

    private fun updateSuccessState(update: (ReaderUiState.Success) -> ReaderUiState.Success) {
        _uiState.update { state ->
            if (state is ReaderUiState.Success) update(state) else state
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (isTtsBound) {
            try {
                context.unbindService(ttsConnection)
            } catch (e: Exception) {
                Timber.e(e, "Failed to unbind TTS service in onCleared")
            }
        }
        viewModelScope.launch {
            try {
                val state = _uiState.value as? ReaderUiState.Success
                if (state != null) {
                    bookRepository.updateReadingProgress(bookId, state.currentPage)
                }
                val pagesRead = ((state?.currentPage ?: 0) - startPage).coerceAtLeast(0)
                if (sessionId > 0) {
                    readingStatsRepository.endSession(sessionId, pagesRead)
                }
                pdfRenderCache.close()
            } catch (e: Exception) {
                Timber.e(e, "Error in onCleared")
            }
        }
    }

    fun updateZoomLevel(zoom: Float) {
        viewModelScope.launch {
            userPreferences.setBookZoomLevel(bookId, zoom)
        }
    }

    // TTS Control Methods
    fun startTts() {
        val state = _uiState.value as? ReaderUiState.Success ?: return
        viewModelScope.launch {
            val text = if (state.pageText.isBlank()) {
                val extracted = try {
                    bookRepository.extractPageText(state.book.filePath, state.currentPage)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to extract text for TTS")
                    ""
                }
                updateSuccessState { it.copy(pageText = extracted) }
                extracted
            } else {
                state.pageText
            }

            if (text.isNotBlank()) {
                val intent = Intent(context, TtsService::class.java)
                context.startService(intent)
                context.bindService(intent, ttsConnection, Context.BIND_AUTO_CREATE)
                
                updateSuccessState { it.copy(isTtsActive = true) }
                
                if (isTtsBound) {
                    ttsService?.startSpeaking(text)
                } else {
                    pendingTextToSpeak = text
                }
            }
        }
    }

    fun pauseTts() {
        if (isTtsBound) {
            ttsService?.pauseSpeaking()
        }
    }

    fun resumeTts() {
        if (isTtsBound) {
            ttsService?.resumeSpeaking()
        }
    }

    fun stopTts() {
        if (isTtsBound) {
            ttsService?.stopSpeaking()
            try {
                context.unbindService(ttsConnection)
            } catch (e: Exception) {
                Timber.e(e, "Failed to unbind TTS service")
            }
            isTtsBound = false
            ttsService = null
        }
        updateSuccessState { 
            it.copy(
                isTtsActive = false, 
                isTtsPlaying = false, 
                ttsWordRange = null 
            ) 
        }
    }

    // Summarizer Methods
    fun summarizeCurrentPage() {
        val state = _uiState.value as? ReaderUiState.Success ?: return
        
        updateSuccessState { 
            it.copy(
                isSummarizing = true,
                summaryText = "",
                summaryError = null
            ) 
        }

        viewModelScope.launch {
            try {
                summarizePageUseCase(state.book.filePath, state.currentPage)
                    .collect { chunk ->
                        updateSuccessState { 
                            it.copy(
                                summaryText = it.summaryText + chunk
                            ) 
                        }
                    }
                updateSuccessState { it.copy(isSummarizing = false) }
            } catch (e: Exception) {
                Timber.e(e, "Failed to summarize page")
                updateSuccessState { 
                    it.copy(
                        isSummarizing = false,
                        summaryError = e.message ?: "Failed to generate summary"
                    ) 
                }
            }
        }
    }

    fun clearSummary() {
        updateSuccessState { 
            it.copy(
                summaryText = "",
                summaryError = null,
                isSummarizing = false
            ) 
        }
    }
}
