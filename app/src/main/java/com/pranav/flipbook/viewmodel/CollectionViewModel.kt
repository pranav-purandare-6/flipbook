package com.pranav.flipbook.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pranav.flipbook.FlipBookApplication
import com.pranav.flipbook.data.entity.BookEntity
import com.pranav.flipbook.data.entity.CollectionEntity
import com.pranav.flipbook.data.repository.BookRepository
import com.pranav.flipbook.data.repository.CollectionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CollectionViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as FlipBookApplication).database
    private val collectionRepo = CollectionRepository(db.collectionDao())
    private val bookRepo = BookRepository(db.bookDao())

    val collections: StateFlow<List<CollectionEntity>> = collectionRepo.getAllCollections()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun getBooksInCollection(collectionId: Long): Flow<List<BookEntity>> =
        collectionRepo.getBooksInCollection(collectionId)

    fun getBookCount(collectionId: Long): Flow<Int> =
        collectionRepo.getBookCountInCollection(collectionId)

    fun createCollection(name: String, description: String? = null) {
        viewModelScope.launch {
            collectionRepo.insertCollection(
                CollectionEntity(name = name, description = description)
            )
        }
    }

    fun renameCollection(collection: CollectionEntity, newName: String) {
        viewModelScope.launch {
            collectionRepo.updateCollection(collection.copy(name = newName))
        }
    }

    fun deleteCollection(collection: CollectionEntity) {
        viewModelScope.launch {
            collectionRepo.deleteCollection(collection)
        }
    }

    fun addBookToCollection(bookId: Long, collectionId: Long) {
        viewModelScope.launch {
            collectionRepo.addBookToCollection(bookId, collectionId)
        }
    }

    fun removeBookFromCollection(bookId: Long, collectionId: Long) {
        viewModelScope.launch {
            collectionRepo.removeBookFromCollection(bookId, collectionId)
        }
    }

    fun getAllBooks(): Flow<List<BookEntity>> = bookRepo.getAllBooks()
}
