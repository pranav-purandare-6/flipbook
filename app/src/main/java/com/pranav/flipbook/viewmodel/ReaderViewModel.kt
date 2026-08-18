package com.pranav.flipbook.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pranav.flipbook.FlipBookApplication
import com.pranav.flipbook.data.entity.*
import com.pranav.flipbook.data.repository.*
import com.pranav.flipbook.pdf.renderer.BitmapAppearanceProcessor
import com.pranav.flipbook.pdf.renderer.PageBitmapCache
import com.pranav.flipbook.pdf.renderer.PdfRendererManager
import com.pranav.flipbook.pdf.metadata.PdfMetadataExtractor
import com.pranav.flipbook.pdf.metadata.TocEntry
import com.pranav.flipbook.pdf.search.PdfTextSearchEngine
import com.pranav.flipbook.pdf.search.SearchResult
import com.pranav.flipbook.ui.reader.ReaderAppearance
import com.pranav.flipbook.ui.reader.ReaderMode
import com.pranav.flipbook.ui.reader.pagecurl.PageTransitionStyle
import com.pranav.flipbook.utils.calculateProgress
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ReaderViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FlipBookApplication
    private val db = app.database
    private val audioManager = app.audioManager
    private val dataStore = application.settingsDataStore

    private val bookRepo = BookRepository(db.bookDao())
    private val bookmarkRepo = BookmarkRepository(db.bookmarkDao())
    private val sessionRepo = ReadingSessionRepository(db.readingSessionDao())
    private val favoriteQuoteRepo = FavoriteQuoteRepository(db.favoriteQuoteDao())
    private val highlightRepo = HighlightRepository(db.highlightDao())

    private val pdfRenderer = PdfRendererManager(application)
    val pageCache = PageBitmapCache()
    private val metadataExtractor = PdfMetadataExtractor(application)
    private val searchEngine = PdfTextSearchEngine(application)

    private val _book = MutableStateFlow<BookEntity?>(null)
    val book: StateFlow<BookEntity?> = _book.asStateFlow()

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _totalPages = MutableStateFlow(0)
    val totalPages: StateFlow<Int> = _totalPages.asStateFlow()

    private val _currentBitmap = MutableStateFlow<Bitmap?>(null)
    val currentBitmap: StateFlow<Bitmap?> = _currentBitmap.asStateFlow()

    private val _nextBitmap = MutableStateFlow<Bitmap?>(null)
    val nextBitmap: StateFlow<Bitmap?> = _nextBitmap.asStateFlow()

    private val _previousBitmap = MutableStateFlow<Bitmap?>(null)
    val previousBitmap: StateFlow<Bitmap?> = _previousBitmap.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isBookmarked = MutableStateFlow(false)
    val isBookmarked: StateFlow<Boolean> = _isBookmarked.asStateFlow()

    private val _tocEntries = MutableStateFlow<List<TocEntry>>(emptyList())
    val tocEntries: StateFlow<List<TocEntry>> = _tocEntries.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()

    val appearance: StateFlow<ReaderAppearance> = dataStore.data
        .map { prefs ->
            ReaderAppearance(
                mode = ReaderMode.fromKey(prefs[SettingsKeys.READER_THEME] ?: "light"),
                brightness = prefs[SettingsKeys.READER_BRIGHTNESS] ?: 1f,
                marginDp = when (prefs[SettingsKeys.MARGIN_SIZE] ?: "medium") {
                    "small" -> 0
                    "large" -> 24
                    else -> 12
                }
            )
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ReaderAppearance())

    val transitionStyle: StateFlow<PageTransitionStyle> = dataStore.data
        .map { prefs ->
            when (prefs[SettingsKeys.TRANSITION_STYLE] ?: "CURL") {
                "SLIDE" -> PageTransitionStyle.SLIDE
                "FADE" -> PageTransitionStyle.FADE
                "NONE" -> PageTransitionStyle.NONE
                else -> PageTransitionStyle.CURL
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, PageTransitionStyle.CURL)

    val animationDuration: StateFlow<Int> = dataStore.data
        .map { it[SettingsKeys.ANIMATION_SPEED] ?: 400 }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 400)

    val autoHideControls: StateFlow<Boolean> = dataStore.data
        .map { it[SettingsKeys.AUTO_HIDE_CONTROLS] ?: true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private var sessionStartTime: Long = 0L
    private var sessionStartPage: Int = 0
    private var currentBookId: Long = 0L
    private var currentUri: Uri? = null
    private var renderJob: Job? = null
    private var bookmarkCheckJob: Job? = null
    private var pageWidth = 1080
    private var pageHeight = 1920
    private var appearanceKey: String = ""
    private var readerEntered = false

    init {
        viewModelScope.launch {
            dataStore.data.collect { prefs ->
                audioManager.updateSettings(
                    pageSound = prefs[SettingsKeys.PAGE_SOUND] ?: false,
                    pageVolume = prefs[SettingsKeys.PAGE_SOUND_VOLUME] ?: 0.6f,
                    ambient = prefs[SettingsKeys.AMBIENT_SOUND] ?: "none",
                    ambientVol = prefs[SettingsKeys.AMBIENT_VOLUME] ?: 0.5f
                )
            }
        }

        viewModelScope.launch {
            appearance.collect { app ->
                if (app.cacheKey != appearanceKey && _book.value != null) {
                    appearanceKey = app.cacheKey
                    reapplyAppearance()
                }
            }
        }
    }

    fun openBook(bookId: Long) {
        if (currentBookId == bookId && _book.value != null) return
        currentBookId = bookId
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val bookEntity = bookRepo.getBookById(bookId)
                if (bookEntity == null) {
                    _error.value = "Book not found"
                    _isLoading.value = false
                    return@launch
                }

                _book.value = bookEntity
                currentUri = Uri.parse(bookEntity.uri)

                val opened = pdfRenderer.open(currentUri!!)
                if (!opened) {
                    _error.value = "Unable to open PDF. The file may have been moved or deleted."
                    _isLoading.value = false
                    return@launch
                }

                val pages = pdfRenderer.pageCount
                _totalPages.value = pages

                if (bookEntity.pageCount != pages) {
                    bookRepo.updatePageCount(bookId, pages)
                }

                val savedPage = bookEntity.currentPage.coerceIn(0, (pages - 1).coerceAtLeast(0))
                _currentPage.value = savedPage

                sessionStartTime = System.currentTimeMillis()
                sessionStartPage = savedPage

                bookRepo.updateProgress(bookId, savedPage, calculateProgress(savedPage, pages))

                launch {
                    _tocEntries.value = metadataExtractor.extractTableOfContents(currentUri!!)
                }

                if (!readerEntered) {
                    audioManager.onReaderEnter()
                    readerEntered = true
                }

                renderCurrentPage()
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Error opening book: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun onPageTurnStart() {
        audioManager.onPageTurnStart()
    }

    fun onReaderPause() {
        audioManager.onReaderPause()
    }

    fun onReaderResume() {
        audioManager.onReaderResume()
    }

    fun setViewSize(width: Int, height: Int) {
        if (width > 0 && height > 0 && (width != pageWidth || height != pageHeight)) {
            pageWidth = width
            pageHeight = height
            pageCache.evictAll()
            renderCurrentPage()
        }
    }

    private fun renderCurrentPage() {
        renderJob?.cancel()
        renderJob = viewModelScope.launch {
            val page = _currentPage.value
            val total = _totalPages.value
            val app = appearance.value

            _currentBitmap.value = loadProcessedPage(page, app)
            _nextBitmap.value = if (page + 1 < total) loadProcessedPage(page + 1, app) else null
            _previousBitmap.value = if (page - 1 >= 0) loadProcessedPage(page - 1, app) else null

            pageCache.preloadPages(page, total, pdfRenderer, pageWidth, pageHeight)
            pageCache.evictDistant(page)
        }

        bookmarkCheckJob?.cancel()
        bookmarkCheckJob = viewModelScope.launch {
            bookmarkRepo.isPageBookmarked(currentBookId, _currentPage.value)
                .collect { _isBookmarked.value = it }
        }
    }

    private suspend fun loadProcessedPage(page: Int, app: ReaderAppearance): Bitmap? {
        val raw = pageCache.get(page) ?: pdfRenderer.renderPage(page, pageWidth, pageHeight)
        raw?.let { pageCache.put(page, it) }
        return raw?.let { BitmapAppearanceProcessor.apply(it, app) }
    }

    private fun reapplyAppearance() {
        renderJob?.cancel()
        renderJob = viewModelScope.launch {
            val page = _currentPage.value
            val total = _totalPages.value
            val app = appearance.value
            _currentBitmap.value = loadProcessedPage(page, app)
            _nextBitmap.value = if (page + 1 < total) loadProcessedPage(page + 1, app) else null
            _previousBitmap.value = if (page - 1 >= 0) loadProcessedPage(page - 1, app) else null
        }
    }

    fun goToPage(page: Int) {
        val target = page.coerceIn(0, (_totalPages.value - 1).coerceAtLeast(0))
        if (target == _currentPage.value) return
        _currentPage.value = target
        renderCurrentPage()
        saveProgress()
    }

    fun nextPage() {
        if (_currentPage.value < _totalPages.value - 1) {
            _currentPage.value++
            renderCurrentPage()
            saveProgress()
        }
    }

    fun previousPage() {
        if (_currentPage.value > 0) {
            _currentPage.value--
            renderCurrentPage()
            saveProgress()
        }
    }

    private fun saveProgress() {
        viewModelScope.launch {
            val page = _currentPage.value
            val total = _totalPages.value
            bookRepo.updateProgress(currentBookId, page, calculateProgress(page, total))
            if (page >= total - 1 && total > 0) {
                bookRepo.updateCompleted(currentBookId, true)
            }
        }
    }

    fun toggleBookmark() {
        viewModelScope.launch {
            bookmarkRepo.toggleBookmark(currentBookId, _currentPage.value)
        }
    }

    fun saveFavoriteQuote(text: String) {
        if (text.isBlank() || currentBookId == 0L) return
        viewModelScope.launch {
            favoriteQuoteRepo.insertQuote(
                FavoriteQuoteEntity(
                    bookId = currentBookId,
                    page = _currentPage.value,
                    text = text.trim()
                )
            )
        }
    }

    fun savePageHighlight(text: String, color: Int) {
        if (text.isBlank() || currentBookId == 0L) return
        viewModelScope.launch {
            highlightRepo.insertHighlight(
                HighlightEntity(
                    bookId = currentBookId,
                    page = _currentPage.value,
                    text = text.trim(),
                    color = color
                )
            )
        }
    }

    fun searchInPdf(query: String) {
        viewModelScope.launch {
            currentUri?.let { uri ->
                _searchResults.value = if (query.isBlank()) emptyList()
                else searchEngine.search(uri, query)
            }
        }
    }

    fun endSession() {
        if (readerEntered) {
            audioManager.onReaderExit()
            readerEntered = false
        }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val duration = now - sessionStartTime
            val pagesRead = kotlin.math.abs(_currentPage.value - sessionStartPage)
            if (duration > 5000 && pagesRead > 0) {
                sessionRepo.insertSession(
                    ReadingSessionEntity(
                        bookId = currentBookId,
                        startTime = sessionStartTime,
                        endTime = now,
                        startPage = sessionStartPage,
                        endPage = _currentPage.value,
                        pagesRead = pagesRead,
                        duration = duration
                    )
                )
                bookRepo.addReadingStats(currentBookId, duration, pagesRead)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        endSession()
        viewModelScope.launch { pdfRenderer.close() }
        pageCache.destroy()
    }
}
