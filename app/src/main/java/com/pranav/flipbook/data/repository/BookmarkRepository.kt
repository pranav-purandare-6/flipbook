package com.pranav.flipbook.data.repository

import com.pranav.flipbook.data.dao.BookmarkDao
import com.pranav.flipbook.data.entity.BookmarkEntity
import kotlinx.coroutines.flow.Flow

class BookmarkRepository(private val bookmarkDao: BookmarkDao) {

    fun getBookmarksForBook(bookId: Long): Flow<List<BookmarkEntity>> =
        bookmarkDao.getBookmarksForBook(bookId)

    fun getAllBookmarks(): Flow<List<BookmarkEntity>> = bookmarkDao.getAllBookmarks()

    fun isPageBookmarked(bookId: Long, page: Int): Flow<Boolean> =
        bookmarkDao.isPageBookmarked(bookId, page)

    fun getBookmarkCountForBook(bookId: Long): Flow<Int> =
        bookmarkDao.getBookmarkCountForBook(bookId)

    suspend fun getBookmarkById(id: Long): BookmarkEntity? = bookmarkDao.getBookmarkById(id)

    suspend fun getBookmarkForPage(bookId: Long, page: Int): BookmarkEntity? =
        bookmarkDao.getBookmarkForPage(bookId, page)

    suspend fun insertBookmark(bookmark: BookmarkEntity): Long = bookmarkDao.insert(bookmark)

    suspend fun updateBookmark(bookmark: BookmarkEntity) = bookmarkDao.update(bookmark)

    suspend fun deleteBookmark(bookmark: BookmarkEntity) = bookmarkDao.delete(bookmark)

    suspend fun toggleBookmark(bookId: Long, page: Int): Boolean {
        val existing = bookmarkDao.getBookmarkForPage(bookId, page)
        return if (existing != null) {
            bookmarkDao.deleteBookmarkForPage(bookId, page)
            false
        } else {
            bookmarkDao.insert(BookmarkEntity(bookId = bookId, page = page))
            true
        }
    }
}
