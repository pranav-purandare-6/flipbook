package com.pranav.flipbook.data.repository

import com.pranav.flipbook.data.dao.CollectionDao
import com.pranav.flipbook.data.entity.BookCollectionCrossRef
import com.pranav.flipbook.data.entity.BookEntity
import com.pranav.flipbook.data.entity.CollectionEntity
import kotlinx.coroutines.flow.Flow

class CollectionRepository(private val collectionDao: CollectionDao) {

    fun getAllCollections(): Flow<List<CollectionEntity>> = collectionDao.getAllCollections()

    fun getBooksInCollection(collectionId: Long): Flow<List<BookEntity>> =
        collectionDao.getBooksInCollection(collectionId)

    fun getCollectionsForBook(bookId: Long): Flow<List<CollectionEntity>> =
        collectionDao.getCollectionsForBook(bookId)

    fun getBookCountInCollection(collectionId: Long): Flow<Int> =
        collectionDao.getBookCountInCollection(collectionId)

    suspend fun getCollectionById(id: Long): CollectionEntity? = collectionDao.getCollectionById(id)

    suspend fun insertCollection(collection: CollectionEntity): Long =
        collectionDao.insertCollection(collection)

    suspend fun updateCollection(collection: CollectionEntity) =
        collectionDao.updateCollection(collection)

    suspend fun deleteCollection(collection: CollectionEntity) =
        collectionDao.deleteCollection(collection)

    suspend fun addBookToCollection(bookId: Long, collectionId: Long) =
        collectionDao.addBookToCollection(BookCollectionCrossRef(bookId, collectionId))

    suspend fun removeBookFromCollection(bookId: Long, collectionId: Long) =
        collectionDao.removeBookFromCollection(BookCollectionCrossRef(bookId, collectionId))

    suspend fun isBookInCollection(bookId: Long, collectionId: Long): Boolean =
        collectionDao.isBookInCollection(bookId, collectionId)
}
