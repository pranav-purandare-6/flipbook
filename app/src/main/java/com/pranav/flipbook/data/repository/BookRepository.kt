package com.pranav.flipbook.data.repository

import com.pranav.flipbook.data.dao.BookDao
import com.pranav.flipbook.data.entity.BookEntity
import kotlinx.coroutines.flow.Flow

class BookRepository(private val bookDao: BookDao) {

    fun getAllBooks(): Flow<List<BookEntity>> = bookDao.getAllBooks()

    fun getFavoriteBooks(): Flow<List<BookEntity>> = bookDao.getFavoriteBooks()

    fun getRecentlyOpened(limit: Int = 10): Flow<List<BookEntity>> = bookDao.getRecentlyOpened(limit)

    fun getRecentlyAdded(limit: Int = 10): Flow<List<BookEntity>> = bookDao.getRecentlyAdded(limit)

    fun getContinueReading(limit: Int = 5): Flow<List<BookEntity>> = bookDao.getContinueReading(limit)

    fun searchBooks(query: String): Flow<List<BookEntity>> = bookDao.searchBooks(query)

    fun getBookByIdFlow(bookId: Long): Flow<BookEntity?> = bookDao.getBookByIdFlow(bookId)

    fun getBookCount(): Flow<Int> = bookDao.getBookCount()

    fun getCompletedBookCount(): Flow<Int> = bookDao.getCompletedBookCount()

    fun getTotalPagesRead(): Flow<Int?> = bookDao.getTotalPagesRead()

    fun getTotalReadingTime(): Flow<Long?> = bookDao.getTotalReadingTime()

    // Sorting
    fun getBooksSortedByNameAsc(): Flow<List<BookEntity>> = bookDao.getBooksSortedByNameAsc()
    fun getBooksSortedByNameDesc(): Flow<List<BookEntity>> = bookDao.getBooksSortedByNameDesc()
    fun getBooksSortedBySize(): Flow<List<BookEntity>> = bookDao.getBooksSortedBySize()
    fun getBooksSortedByProgress(): Flow<List<BookEntity>> = bookDao.getBooksSortedByProgress()

    suspend fun getBookById(bookId: Long): BookEntity? = bookDao.getBookById(bookId)

    suspend fun getBookByUri(uri: String): BookEntity? = bookDao.getBookByUri(uri)

    suspend fun insertBook(book: BookEntity): Long = bookDao.insert(book)

    suspend fun updateBook(book: BookEntity) = bookDao.update(book)

    suspend fun deleteBook(book: BookEntity) = bookDao.delete(book)

    suspend fun updateProgress(bookId: Long, page: Int, progress: Float) =
        bookDao.updateProgress(bookId, page, progress)

    suspend fun updateFavorite(bookId: Long, isFavorite: Boolean) =
        bookDao.updateFavorite(bookId, isFavorite)

    suspend fun updateCustomTitle(bookId: Long, title: String?) =
        bookDao.updateCustomTitle(bookId, title)

    suspend fun updateCoverPath(bookId: Long, path: String) =
        bookDao.updateCoverPath(bookId, path)

    suspend fun updatePageCount(bookId: Long, count: Int) =
        bookDao.updatePageCount(bookId, count)

    suspend fun addReadingStats(bookId: Long, duration: Long, pages: Int) =
        bookDao.addReadingStats(bookId, duration, pages)

    suspend fun updateCompleted(bookId: Long, completed: Boolean) =
        bookDao.updateCompleted(bookId, completed)
}
