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
import com.example.pdfreader.domain.model.ReadingDirection
import com.example.pdfreader.domain.repository.GeminiRepository
import com.example.pdfreader.data.local.dao.BookPageDao
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

import com.example.pdfreader.domain.usecase.SummarizePageUseCase
import dagger.hilt.android.qualifiers.ApplicationContext

data class ChatMessage(
    val sender: String, // "user" or "ai"
    val message: String
)

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
        val pageText: String = "",
        val zoomLevel: Float = 1f,
        val readingDirection: ReadingDirection = ReadingDirection.LTR,
        val isAskingQuestion: Boolean = false,
        val chatMessages: List<ChatMessage> = emptyList(),
        val questionError: String? = null,
        val isReflowMode: Boolean = false,
        val reflowText: String = "",
        val isExtractingReflow: Boolean = false,
        val reflowFontSize: Float = 18f,
        val reflowLineSpacing: Float = 1.4f,
        val reflowLetterSpacing: Float = 0f,
        val reflowFontFamily: String = "Lora",
        val isAutoCropEnabled: Boolean = false
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
    private val geminiRepository: GeminiRepository,
    private val bookPageDao: BookPageDao,
    @ApplicationContext private val context: Context
) : ViewModel() {



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
                    if (bookId > 0) {
                        if (kotlin.math.abs(page - lastSavedPage) >= 3 || page == 0 || page == (uiState.value as? ReaderUiState.Success)?.totalPages?.minus(1)) {
                            bookRepository.updateReadingProgress(bookId, page)
                            lastSavedPage = page
                        }
                    }
                }
        }
    }

    private fun copyUriToTempFile(uriStr: String): String? {
        return try {
            val uri = android.net.Uri.parse(uriStr)
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val tempFile = java.io.File(context.cacheDir, "transient_book.pdf")
            if (tempFile.exists()) {
                tempFile.delete()
            }
            tempFile.outputStream().use { outputStream ->
                inputStream.use { it.copyTo(outputStream) }
            }
            tempFile.absolutePath
        } catch (e: Exception) {
            Timber.e(e, "Failed to copy external PDF URI to temp file")
            null
        }
    }

    private fun getFileNameFromUri(uriStr: String): String? {
        return try {
            val uri = android.net.Uri.parse(uriStr)
            if (uri.scheme == "content") {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) cursor.getString(nameIndex) else null
                    } else null
                }
            } else {
                uri.path?.let { java.io.File(it).name }
            }
        } catch (e: Exception) {
            null
        }
    }

    fun setScreenWidth(width: Int) {
        screenWidth = width
    }

    private fun loadBook() {
        viewModelScope.launch {
            try {
                val book: Book
                val uriString: String? = savedStateHandle.get<String>("uri")

                if (bookId == -1L || uriString != null) {
                    val realUri = uriString ?: throw Exception("No file URI supplied")
                    val tempPath = withContext(Dispatchers.IO) { copyUriToTempFile(realUri) }
                        ?: throw Exception("Failed to open external PDF document")

                    book = Book(
                        id = -1L,
                        filePath = tempPath,
                        title = getFileNameFromUri(realUri) ?: "External Document",
                        author = "Transient Mode"
                    )
                } else {
                    val dbBook = bookRepository.getBookById(bookId)
                    if (dbBook == null) {
                        try {
                            userPreferences.setLastOpenedBookId(-1L)
                        } catch (_: Exception) {}
                        _uiState.value = ReaderUiState.Error("Book not found")
                        return@launch
                    }
                    book = dbBook
                }

                val pageCount = withContext(Dispatchers.IO) {
                    pdfRenderCache.openPdf(book.filePath)
                }

                val currentPage = book.currentPage.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
                startPage = currentPage

                val initialZoom = if (bookId > 0) {
                    try {
                        userPreferences.getBookZoomLevel(bookId).first()
                    } catch (_: Exception) {
                        1f
                    }
                } else {
                    1f
                }

                _uiState.value = ReaderUiState.Success(
                    book = book,
                    currentPage = currentPage,
                    totalPages = pageCount,
                    zoomLevel = initialZoom
                )

                if (bookId > 0) {
                    // Start reading session
                    sessionId = readingStatsRepository.startSession(bookId)

                    // Update last opened
                    userPreferences.setLastOpenedBookId(bookId)
                    bookRepository.updateReadingProgress(bookId, currentPage)
                    lastSavedPage = currentPage
                }

                // Render current page
                renderPage(currentPage)

                // Observe bookmark status
                if (bookId > 0) {
                    observeBookmark(currentPage)
                }

                // Prefetch adjacent pages
                prefetchAdjacentPages(currentPage)

            } catch (e: Exception) {
                Timber.e(e, "Failed to load book")
                if (bookId > 0) {
                    try {
                        userPreferences.setLastOpenedBookId(-1L)
                    } catch (_: Exception) {}
                }
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
            userPreferences.readingDirection.collectLatest { dir ->
                updateSuccessState { it.copy(readingDirection = try { ReadingDirection.valueOf(dir) } catch (_: Exception) { ReadingDirection.LTR }) }
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

            updateSuccessState { 
                it.copy(
                    currentPage = validPage,
                    pageText = "",
                    summaryText = "",
                    summaryError = null,
                    isSummarizing = false
                ) 
            }
            renderPage(validPage)
            if (bookId > 0) {
                observeBookmark(validPage)
            }
            prefetchAdjacentPages(validPage)

            if (state.isReflowMode) {
                loadReflowTextForPage()
            }

            // Save progress via debounced flow
            if (bookId > 0) {
                progressSaveFlow.emit(validPage)
            }
        }
    }

    private fun prefetchAdjacentPages(pageIndex: Int) {
        val state = _uiState.value as? ReaderUiState.Success ?: return
        val totalPages = state.totalPages
        val theme = state.theme
        val currentScreenWidth = screenWidth

        viewModelScope.launch(Dispatchers.IO) {
            // Prefetch next page
            if (pageIndex + 1 < totalPages) {
                try {
                    pdfRenderCache.renderPage(pageIndex + 1, currentScreenWidth, theme)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to prefetch next page ${pageIndex + 1}")
                }
            }
            // Prefetch previous page
            if (pageIndex - 1 >= 0) {
                try {
                    pdfRenderCache.renderPage(pageIndex - 1, currentScreenWidth, theme)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to prefetch previous page ${pageIndex - 1}")
                }
            }
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
            if (bookId > 0) {
                val state = _uiState.value as? ReaderUiState.Success ?: return@launch
                bookmarkRepository.toggleBookmark(bookId, state.currentPage)
            }
        }
    }

    fun toggleScrollMode() {
        updateSuccessState { it.copy(isVerticalScroll = !it.isVerticalScroll) }
    }

    private suspend fun renderPage(pageIndex: Int) {
        try {
            val state = _uiState.value as? ReaderUiState.Success ?: return
            val theme = state.theme
            val autoCrop = state.isAutoCropEnabled
            val bitmap = if (autoCrop) {
                pdfRenderCache.renderPageAutoCropped(pageIndex, screenWidth, theme)
            } else {
                pdfRenderCache.renderPage(pageIndex, screenWidth, theme)
            }
            updateSuccessState { it.copy(currentBitmap = bitmap) }
        } catch (e: Exception) {
            Timber.e(e, "Failed to render page $pageIndex")
        }
    }

    fun renderPageForPager(pageIndex: Int, callback: (Bitmap?) -> Unit) {
        viewModelScope.launch {
            try {
                val state = _uiState.value as? ReaderUiState.Success ?: return@launch
                val theme = state.theme
                val autoCrop = state.isAutoCropEnabled
                val bitmap = if (autoCrop) {
                    pdfRenderCache.renderPageAutoCropped(pageIndex, screenWidth, theme)
                } else {
                    pdfRenderCache.renderPage(pageIndex, screenWidth, theme)
                }
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
        viewModelScope.launch {
            try {
                val state = _uiState.value as? ReaderUiState.Success
                if (state != null && bookId > 0) {
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
        updateSuccessState { it.copy(zoomLevel = zoom) }
        viewModelScope.launch {
            if (bookId > 0) {
                userPreferences.setBookZoomLevel(bookId, zoom)
            }
        }
    }

    fun toggleAutoCrop() {
        val state = _uiState.value as? ReaderUiState.Success ?: return
        val newVal = !state.isAutoCropEnabled
        updateSuccessState { it.copy(isAutoCropEnabled = newVal) }
        viewModelScope.launch {
            renderPage(state.currentPage)
        }
    }

    fun toggleReflowMode() {
        val state = _uiState.value as? ReaderUiState.Success ?: return
        val newReflow = !state.isReflowMode
        updateSuccessState { it.copy(isReflowMode = newReflow) }
        if (newReflow) {
            loadReflowTextForPage()
        }
    }

    fun updateReflowSettings(fontSize: Float, lineSpacing: Float, letterSpacing: Float, fontFamily: String) {
        updateSuccessState {
            it.copy(
                reflowFontSize = fontSize,
                reflowLineSpacing = lineSpacing,
                reflowLetterSpacing = letterSpacing,
                reflowFontFamily = fontFamily
            )
        }
    }

    fun loadReflowTextForPage() {
        val state = _uiState.value as? ReaderUiState.Success ?: return
        
        updateSuccessState {
            it.copy(
                isExtractingReflow = true,
                reflowText = ""
            )
        }
        
        viewModelScope.launch {
            try {
                var text = ""
                if (bookId > 0) {
                    val dbPage = bookPageDao.getPageText(bookId, state.currentPage)
                    if (dbPage != null) {
                        text = dbPage.pageText
                    }
                }
                
                if (text.isBlank()) {
                    text = withContext(Dispatchers.IO) {
                        bookRepository.extractPageText(state.book.filePath, state.currentPage)
                    }
                }
                
                updateSuccessState {
                    it.copy(
                        isExtractingReflow = false,
                        reflowText = text
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to extract reflow text")
                updateSuccessState {
                    it.copy(
                        isExtractingReflow = false,
                        reflowText = "Failed to extract text for reflow mode: ${e.message}"
                    )
                }
            }
        }
    }

    fun askBookQuestion(question: String) {
        val state = _uiState.value as? ReaderUiState.Success ?: return
        if (bookId == -1L) {
            updateSuccessState { 
                it.copy(
                    questionError = "Q&A is not supported for external, transient PDF documents."
                ) 
            }
            return
        }
        
        if (question.isBlank()) return

        val newUserMessage = ChatMessage("user", question)
        val updatedMessages = state.chatMessages + newUserMessage

        updateSuccessState {
            it.copy(
                isAskingQuestion = true,
                chatMessages = updatedMessages,
                questionError = null
            )
        }

        viewModelScope.launch {
            try {
                val searchResults = bookPageDao.searchInBook(bookId, question)
                val contextText = if (searchResults.isNotEmpty()) {
                    searchResults.take(5).joinToString("\n\n") { "Page ${it.pageIndex + 1}: ${it.pageText}" }
                } else {
                    val activePage = bookPageDao.getPageText(bookId, state.currentPage)
                    activePage?.pageText ?: ""
                }

                val responseMsgPlaceholder = ChatMessage("ai", "")
                updateSuccessState {
                    it.copy(chatMessages = it.chatMessages + responseMsgPlaceholder)
                }

                geminiRepository.generateAnswerStream(question, contextText)
                    .collect { chunk ->
                        updateSuccessState { s ->
                            val lastMsg = s.chatMessages.lastOrNull()
                            if (lastMsg != null && lastMsg.sender == "ai") {
                                val updatedMsg = lastMsg.copy(message = lastMsg.message + chunk)
                                s.copy(chatMessages = s.chatMessages.dropLast(1) + updatedMsg)
                            } else {
                                s
                            }
                        }
                    }
                updateSuccessState { it.copy(isAskingQuestion = false) }
            } catch (e: Exception) {
                Timber.e(e, "Q&A Chat generation failed")
                updateSuccessState {
                    it.copy(
                        isAskingQuestion = false,
                        questionError = e.message ?: "Failed to generate answer"
                    )
                }
            }
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
