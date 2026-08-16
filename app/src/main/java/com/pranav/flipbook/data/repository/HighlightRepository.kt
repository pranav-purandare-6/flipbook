package com.pranav.flipbook.data.repository

import com.pranav.flipbook.data.dao.HighlightDao
import com.pranav.flipbook.data.dao.PageHighlightCount
import com.pranav.flipbook.data.entity.HighlightEntity
import kotlinx.coroutines.flow.Flow

class HighlightRepository(private val highlightDao: HighlightDao) {

    fun getHighlightsForBook(bookId: Long): Flow<List<HighlightEntity>> =
        highlightDao.getHighlightsForBook(bookId)

    fun getAllHighlights(): Flow<List<HighlightEntity>> = highlightDao.getAllHighlights()

    fun getHighlightsForPage(bookId: Long, page: Int): Flow<List<HighlightEntity>> =
        highlightDao.getHighlightsForPage(bookId, page)

    fun getHighlightCountForBook(bookId: Long): Flow<Int> =
        highlightDao.getHighlightCountForBook(bookId)

    suspend fun getHighlightById(id: Long): HighlightEntity? = highlightDao.getHighlightById(id)

    suspend fun getMostHighlightedPages(bookId: Long, limit: Int = 5): List<PageHighlightCount> =
        highlightDao.getMostHighlightedPages(bookId, limit)

    suspend fun insertHighlight(highlight: HighlightEntity): Long = highlightDao.insert(highlight)

    suspend fun updateHighlight(highlight: HighlightEntity) = highlightDao.update(highlight)

    suspend fun deleteHighlight(highlight: HighlightEntity) = highlightDao.delete(highlight)
}
