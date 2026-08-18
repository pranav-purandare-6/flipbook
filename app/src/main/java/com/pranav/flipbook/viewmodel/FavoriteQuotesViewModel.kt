package com.pranav.flipbook.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pranav.flipbook.FlipBookApplication
import com.pranav.flipbook.data.entity.BookEntity
import com.pranav.flipbook.data.entity.FavoriteQuoteEntity
import com.pranav.flipbook.data.repository.BookRepository
import com.pranav.flipbook.data.repository.FavoriteQuoteRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class FavoriteQuoteItem(
    val quote: FavoriteQuoteEntity,
    val book: BookEntity?
)

@OptIn(ExperimentalCoroutinesApi::class)
class FavoriteQuotesViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as FlipBookApplication).database
    private val quoteRepo = FavoriteQuoteRepository(db.favoriteQuoteDao())
    private val bookRepo = BookRepository(db.bookDao())

    private val query = MutableStateFlow("")
    val searchQuery: StateFlow<String> = query.asStateFlow()

    val quotes: StateFlow<List<FavoriteQuoteItem>> = query
        .flatMapLatest { q ->
            if (q.isBlank()) quoteRepo.getAllQuotes() else quoteRepo.searchQuotes(q)
        }
        .combine(bookRepo.getAllBooks()) { quotes, books ->
            val byId = books.associateBy { it.id }
            quotes.map { FavoriteQuoteItem(it, byId[it.bookId]) }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setSearchQuery(value: String) {
        query.value = value
    }

    fun createQuote(bookId: Long, page: Int, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            quoteRepo.insertQuote(
                FavoriteQuoteEntity(
                    bookId = bookId,
                    page = page.coerceAtLeast(0),
                    text = text.trim()
                )
            )
        }
    }

    fun updateQuote(quote: FavoriteQuoteEntity, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            quoteRepo.updateQuote(quote.copy(text = text.trim()))
        }
    }

    fun deleteQuote(quote: FavoriteQuoteEntity) {
        viewModelScope.launch {
            quoteRepo.deleteQuote(quote)
        }
    }
}
