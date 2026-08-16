package com.pranav.flipbook.data.repository

import com.pranav.flipbook.data.dao.FavoriteQuoteDao
import com.pranav.flipbook.data.entity.FavoriteQuoteEntity
import kotlinx.coroutines.flow.Flow

class FavoriteQuoteRepository(private val quoteDao: FavoriteQuoteDao) {

    fun getAllQuotes(): Flow<List<FavoriteQuoteEntity>> = quoteDao.getAllQuotes()

    fun getQuotesForBook(bookId: Long): Flow<List<FavoriteQuoteEntity>> =
        quoteDao.getQuotesForBook(bookId)

    fun searchQuotes(query: String): Flow<List<FavoriteQuoteEntity>> = quoteDao.searchQuotes(query)

    suspend fun getQuoteById(id: Long): FavoriteQuoteEntity? = quoteDao.getQuoteById(id)

    suspend fun insertQuote(quote: FavoriteQuoteEntity): Long = quoteDao.insert(quote)

    suspend fun updateQuote(quote: FavoriteQuoteEntity) = quoteDao.update(quote)

    suspend fun deleteQuote(quote: FavoriteQuoteEntity) = quoteDao.delete(quote)
}
