package com.pranav.flipbook.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pranav.flipbook.FlipBookApplication
import com.pranav.flipbook.data.entity.BookEntity
import com.pranav.flipbook.data.entity.ReadingListEntity
import com.pranav.flipbook.data.repository.BookRepository
import com.pranav.flipbook.data.repository.ReadingListRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ReadingListViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as FlipBookApplication).database
    private val repo = ReadingListRepository(db.readingListDao())
    private val bookRepo = BookRepository(db.bookDao())

    val lists: StateFlow<List<ReadingListEntity>> = repo.getAllLists()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allBooks: StateFlow<List<BookEntity>> = bookRepo.getAllBooks()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch { repo.initializeDefaultLists() }
    }

    fun getBooksInList(listId: Long): StateFlow<List<BookEntity>> =
        repo.getBooksInList(listId)
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun createList(name: String) {
        viewModelScope.launch { repo.createList(name) }
    }

    fun renameList(list: ReadingListEntity, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repo.updateList(list.copy(name = name.trim(), modifiedDate = System.currentTimeMillis()))
        }
    }

    fun deleteList(list: ReadingListEntity) {
        if (list.isSystemList) return
        viewModelScope.launch { repo.deleteList(list) }
    }

    fun addBookToList(listId: Long, bookId: Long) {
        viewModelScope.launch { repo.addBookToList(listId, bookId) }
    }

    fun removeBookFromList(listId: Long, bookId: Long) {
        viewModelScope.launch { repo.removeBookFromList(listId, bookId) }
    }

    fun toggleFavorite(book: BookEntity) {
        viewModelScope.launch { bookRepo.updateFavorite(book.id, !book.isFavorite) }
    }
}
