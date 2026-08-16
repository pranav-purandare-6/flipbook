package com.pranav.flipbook.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pranav.flipbook.FlipBookApplication
import com.pranav.flipbook.data.entity.*
import com.pranav.flipbook.data.repository.*
import com.pranav.flipbook.pdf.renderer.PageBitmapCache
import com.pranav.flipbook.pdf.renderer.PdfRendererManager
import com.pranav.flipbook.pdf.metadata.PdfMetadataExtractor
import com.pranav.flipbook.pdf.metadata.TocEntry
import com.pranav.flipbook.pdf.search.PdfTextSearchEngine
import com.pranav.flipbook.pdf.search.SearchResult
import com.pranav.flipbook.ui.reader.pagecurl.PageTransitionStyle
import com.pranav.flipbook.utils.calculateProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReaderViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as FlipBookApplication).database
    private val bookRepo = BookRepository(db.bookDao())
    private val bookmarkRepo = BookmarkRepository(db.bookmarkDao())
    private val sessionRepo = ReadingSessionRepository(db.readingSessionDao())
    private val noteRepo = NoteRepository(db.noteDao())
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

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _transitionStyle = MutableStateFlow(PageTransitionStyle.CURL)
    val transitionStyle: StateFlow<PageTransitionStyle> = _transitionStyle.asStateFlow()

    private val _animationDuration = MutableStateFlow(400)
    val animationDuration: StateFlow<Int> = _animationDuration.asStateFlow()

    private var sessionStartTime: Long = 0L
    private var sessionStartPage: Int = 0
    private var currentBookId: Long = 0L
    private var currentUri: Uri? = null
    private var renderJob: Job? = null
    private var pageWidth = 1080
    private var pageHeight = 1920
    private var bookmarkCheckJob: Job? = null

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

                // Restore saved page (ONCE, not in LaunchedEffect with pageIndex)
                val savedPage = bookEntity.currentPage.coerceIn(0, (pages - 1).coerceAtLeast(0))
                _currentPage.value = savedPage

                // Start reading session
                sessionStartTime = System.currentTimeMillis()
                sessionStartPage = savedPage

                // Update last opened
                bookRepo.updateProgress(
                    bookId, savedPage,
                    calculateProgress(savedPage, pages)
                )

                // Load TOC
                launch {
                    val toc = metadataExtractor.extractTableOfContents(currentUri!!)
                    _tocEntries.value = toc
                }

                // Render current page
                renderCurrentPage()

                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Error opening book: ${e.message}"
                _isLoading.value = false
            }
        }
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

            // Render current page
            val current = pageCache.get(page) ?: pdfRenderer.renderPage(page, pageWidth, pageHeight)
            current?.let {
                pageCache.put(page, it)
                _currentBitmap.value = it
            }

            // Render adjacent pages
            if (page + 1 < total) {
                val next = pageCache.get(page + 1) ?: pdfRenderer.renderPage(page + 1, pageWidth, pageHeight)
                next?.let {
                    pageCache.put(page + 1, it)
                    _nextBitmap.value = it
                }
            } else {
                _nextBitmap.value = null
            }

            if (page - 1 >= 0) {
                val prev = pageCache.get(page - 1) ?: pdfRenderer.renderPage(page - 1, pageWidth, pageHeight)
                prev?.let {
                    pageCache.put(page - 1, it)
                    _previousBitmap.value = it
                }
            } else {
                _previousBitmap.value = null
            }

            // Preload further pages
            pageCache.preloadPages(page, total, pdfRenderer, pageWidth, pageHeight)
            pageCache.evictDistant(page)
        }

        // Check bookmark
        bookmarkCheckJob?.cancel()
        bookmarkCheckJob = viewModelScope.launch {
            bookmarkRepo.isPageBookmarked(currentBookId, _currentPage.value)
                .collect { _isBookmarked.value = it }
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
            val progress = calculateProgress(page, total)
            bookRepo.updateProgress(currentBookId, page, progress)

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

    fun searchInPdf(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            currentUri?.let { uri ->
                val results = searchEngine.search(uri, query)
                _searchResults.value = results
            }
        }
    }

    fun setTransitionStyle(style: PageTransitionStyle) {
        _transitionStyle.value = style
    }

    fun setAnimationDuration(ms: Int) {
        _animationDuration.value = ms.coerceIn(100, 2000)
    }

    fun endSession() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val duration = now - sessionStartTime
            val pagesRead = kotlin.math.abs(_currentPage.value - sessionStartPage)
            if (duration > 5000 && pagesRead > 0) { // At least 5 seconds and 1 page
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
        viewModelScope.launch {
            pdfRenderer.close()
        }
        pageCache.destroy()
    }
}
