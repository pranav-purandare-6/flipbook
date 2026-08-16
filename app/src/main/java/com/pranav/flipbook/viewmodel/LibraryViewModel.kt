package com.pranav.flipbook.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pranav.flipbook.FlipBookApplication
import com.pranav.flipbook.data.entity.BookEntity
import com.pranav.flipbook.data.repository.BookRepository
import com.pranav.flipbook.data.repository.AchievementRepository
import com.pranav.flipbook.pdf.metadata.PdfMetadataExtractor
import com.pranav.flipbook.pdf.thumbnails.ThumbnailManager
import com.pranav.flipbook.utils.getFileName
import com.pranav.flipbook.utils.getFileSize
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi

import java.io.File
import java.io.FileOutputStream

enum class SortOrder {
    RECENTLY_OPENED, NAME_ASC, NAME_DESC, DATE_ADDED, FILE_SIZE, PROGRESS
}

enum class LibraryLayout {
    GRID, LIST, BOOKSHELF
}

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as FlipBookApplication).database
    private val bookRepo = BookRepository(db.bookDao())
    private val achievementRepo = AchievementRepository(db.achievementDao())
    private val thumbnailManager = ThumbnailManager(application)
    private val metadataExtractor = PdfMetadataExtractor(application)

    private val _sortOrder = MutableStateFlow(SortOrder.RECENTLY_OPENED)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _layout = MutableStateFlow(LibraryLayout.GRID)
    val layout: StateFlow<LibraryLayout> = _layout.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _importError = MutableStateFlow<String?>(null)
    val importError: StateFlow<String?> = _importError.asStateFlow()

    val continueReading: StateFlow<List<BookEntity>> = bookRepo.getContinueReading()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val recentlyOpened: StateFlow<List<BookEntity>> = bookRepo.getRecentlyOpened()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val favoriteBooks: StateFlow<List<BookEntity>> = bookRepo.getFavoriteBooks()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allBooks: StateFlow<List<BookEntity>> = combine(
        _sortOrder, _searchQuery
    ) { sort, query ->
        Pair(sort, query)
    }.flatMapLatest { (sort, query) ->
        if (query.isNotBlank()) {
            bookRepo.searchBooks(query)
        } else {
            when (sort) {
                SortOrder.RECENTLY_OPENED -> bookRepo.getRecentlyOpened(1000)
                SortOrder.NAME_ASC -> bookRepo.getBooksSortedByNameAsc()
                SortOrder.NAME_DESC -> bookRepo.getBooksSortedByNameDesc()
                SortOrder.DATE_ADDED -> bookRepo.getAllBooks()
                SortOrder.FILE_SIZE -> bookRepo.getBooksSortedBySize()
                SortOrder.PROGRESS -> bookRepo.getBooksSortedByProgress()
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val bookCount: StateFlow<Int> = bookRepo.getBookCount()
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    init {
        viewModelScope.launch {
            achievementRepo.initializeAchievements()
        }
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun setLayout(layout: LibraryLayout) {
        _layout.value = layout
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun importPdf(sourceUri: Uri) {
        viewModelScope.launch {
            _isImporting.value = true
            _importError.value = null
            try {
                val context = getApplication<FlipBookApplication>()

                val fileName = sourceUri.getFileName(context)
                val fileSize = sourceUri.getFileSize(context)

                // Copy PDF file to app private internal storage to guarantee permanent access & performance
                val booksDir = File(context.filesDir, "books").apply { mkdirs() }
                val internalFile = File(booksDir, "book_${System.currentTimeMillis()}.pdf")

                val copied = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        context.contentResolver.openInputStream(sourceUri)?.use { input ->
                            FileOutputStream(internalFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        true
                    } catch (e: Exception) {
                        e.printStackTrace()
                        false
                    }
                }

                if (!copied || !internalFile.exists() || internalFile.length() == 0L) {
                    _importError.value = "Failed to copy selected PDF file to storage"
                    _isImporting.value = false
                    return@launch
                }

                val savedUriStr = Uri.fromFile(internalFile).toString()

                // Check for duplicate
                val existingBook = bookRepo.getBookByUri(savedUriStr)
                if (existingBook != null) {
                    _importError.value = "This book is already in your library"
                    _isImporting.value = false
                    return@launch
                }

                // Extract metadata from the copied file
                val internalUri = Uri.fromFile(internalFile)
                val metadata = metadataExtractor.extractMetadata(internalUri)

                val now = System.currentTimeMillis()
                val bookEntity = BookEntity(
                    uri = savedUriStr,
                    title = metadata?.title?.takeIf { it.isNotBlank() }
                        ?: fileName.removeSuffix(".pdf").removeSuffix(".PDF"),
                    author = metadata?.author,
                    subject = metadata?.subject,
                    creator = metadata?.creator,
                    producer = metadata?.producer,
                    fileName = fileName,
                    fileSize = internalFile.length(),
                    pageCount = metadata?.pageCount ?: 0,
                    dateAdded = now,
                    lastOpened = now // Initialized so it immediately shows on Home screen under Recently Opened
                )

                val bookId = bookRepo.insertBook(bookEntity)

                // Generate cover from the saved file
                val coverPath = thumbnailManager.generateCover(internalUri, bookId)
                if (coverPath != null) {
                    bookRepo.updateCoverPath(bookId, coverPath)
                }

                // Update achievement
                val count = bookRepo.getBookCount().first()
                achievementRepo.updateProgress("BOOKS_5", count)
                achievementRepo.updateProgress("BOOKS_10", count)
                if (count >= 5) achievementRepo.unlockAchievement("BOOKS_5")
                if (count >= 10) achievementRepo.unlockAchievement("BOOKS_10")

            } catch (e: Exception) {
                _importError.value = "Failed to import PDF: ${e.message}"
            }
            _isImporting.value = false
        }
    }

    fun toggleFavorite(bookId: Long, isFavorite: Boolean) {
        viewModelScope.launch {
            bookRepo.updateFavorite(bookId, !isFavorite)
        }
    }

    fun renameBook(bookId: Long, newTitle: String) {
        viewModelScope.launch {
            bookRepo.updateCustomTitle(bookId, newTitle.takeIf { it.isNotBlank() })
        }
    }

    fun deleteBook(bookId: Long) {
        viewModelScope.launch {
            val book = bookRepo.getBookById(bookId) ?: return@launch
            thumbnailManager.deleteCover(bookId)

            // Delete internal PDF file if stored locally
            if (book.uri.startsWith("file://")) {
                try {
                    val file = File(Uri.parse(book.uri).path ?: "")
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (_: Exception) {}
            }

            bookRepo.deleteBook(book)
        }
    }

    fun clearImportError() {
        _importError.value = null
    }
}
