package com.pranav.flipbook.data.dao

import androidx.room.*
import com.pranav.flipbook.data.entity.BookCollectionCrossRef
import com.pranav.flipbook.data.entity.BookEntity
import com.pranav.flipbook.data.entity.CollectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: CollectionEntity): Long

    @Update
    suspend fun updateCollection(collection: CollectionEntity)

    @Delete
    suspend fun deleteCollection(collection: CollectionEntity)

    @Query("SELECT * FROM collections ORDER BY name ASC")
    fun getAllCollections(): Flow<List<CollectionEntity>>

    @Query("SELECT * FROM collections WHERE id = :id")
    suspend fun getCollectionById(id: Long): CollectionEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addBookToCollection(crossRef: BookCollectionCrossRef)

    @Delete
    suspend fun removeBookFromCollection(crossRef: BookCollectionCrossRef)

    @Query("""
        SELECT b.* FROM books b
        INNER JOIN book_collection_cross_ref bcr ON b.id = bcr.bookId
        WHERE bcr.collectionId = :collectionId
        ORDER BY b.title ASC
    """)
    fun getBooksInCollection(collectionId: Long): Flow<List<BookEntity>>

    @Query("""
        SELECT c.* FROM collections c
        INNER JOIN book_collection_cross_ref bcr ON c.id = bcr.collectionId
        WHERE bcr.bookId = :bookId
        ORDER BY c.name ASC
    """)
    fun getCollectionsForBook(bookId: Long): Flow<List<CollectionEntity>>

    @Query("SELECT COUNT(*) FROM book_collection_cross_ref WHERE collectionId = :collectionId")
    fun getBookCountInCollection(collectionId: Long): Flow<Int>

    @Query("SELECT EXISTS(SELECT 1 FROM book_collection_cross_ref WHERE bookId = :bookId AND collectionId = :collectionId)")
    suspend fun isBookInCollection(bookId: Long, collectionId: Long): Boolean
}
