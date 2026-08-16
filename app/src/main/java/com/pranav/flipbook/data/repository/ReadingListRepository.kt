package com.pranav.flipbook.data.repository

import com.pranav.flipbook.data.dao.ReadingListDao
import com.pranav.flipbook.data.entity.BookEntity
import com.pranav.flipbook.data.entity.ReadingListBookCrossRef
import com.pranav.flipbook.data.entity.ReadingListEntity
import kotlinx.coroutines.flow.Flow

class ReadingListRepository(private val readingListDao: ReadingListDao) {

    fun getAllLists(): Flow<List<ReadingListEntity>> = readingListDao.getAllLists()

    fun getBooksInList(listId: Long): Flow<List<BookEntity>> = readingListDao.getBooksInList(listId)

    fun getBookCountInList(listId: Long): Flow<Int> = readingListDao.getBookCountInList(listId)

    suspend fun getListById(id: Long): ReadingListEntity? = readingListDao.getListById(id)

    suspend fun createList(name: String, description: String? = null, isSystem: Boolean = false): Long {
        return readingListDao.insertList(
            ReadingListEntity(name = name, description = description, isSystemList = isSystem)
        )
    }

    suspend fun updateList(list: ReadingListEntity) = readingListDao.updateList(list)

    suspend fun deleteList(list: ReadingListEntity) = readingListDao.deleteList(list)

    suspend fun addBookToList(listId: Long, bookId: Long) {
        readingListDao.addBookToList(ReadingListBookCrossRef(listId = listId, bookId = bookId))
    }

    suspend fun removeBookFromList(listId: Long, bookId: Long) {
        readingListDao.removeBookFromList(ReadingListBookCrossRef(listId = listId, bookId = bookId))
    }

    suspend fun initializeDefaultLists() {
        if (readingListDao.getListCount() > 0) return
        createList(name = "Currently Reading", description = "Books you are actively reading", isSystem = true)
        createList(name = "Read Later", description = "Books saved for the future", isSystem = true)
        createList(name = "Completed Books", description = "Books you have finished", isSystem = true)
    }
}
