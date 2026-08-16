package com.pranav.flipbook.data.dao

import androidx.room.*
import com.pranav.flipbook.data.entity.BookEntity
import com.pranav.flipbook.data.entity.ReadingListBookCrossRef
import com.pranav.flipbook.data.entity.ReadingListEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingListDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(list: ReadingListEntity): Long

    @Update
    suspend fun updateList(list: ReadingListEntity)

    @Delete
    suspend fun deleteList(list: ReadingListEntity)

    @Query("SELECT * FROM reading_lists ORDER BY isSystemList DESC, name ASC")
    fun getAllLists(): Flow<List<ReadingListEntity>>

    @Query("SELECT * FROM reading_lists WHERE id = :id")
    suspend fun getListById(id: Long): ReadingListEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addBookToList(crossRef: ReadingListBookCrossRef)

    @Delete
    suspend fun removeBookFromList(crossRef: ReadingListBookCrossRef)

    @Query("""
        SELECT b.* FROM books b
        INNER JOIN reading_list_book_cross_ref rlr ON b.id = rlr.bookId
        WHERE rlr.listId = :listId
        ORDER BY rlr.displayOrder ASC, rlr.addedDate DESC
    """)
    fun getBooksInList(listId: Long): Flow<List<BookEntity>>

    @Query("SELECT COUNT(*) FROM reading_list_book_cross_ref WHERE listId = :listId")
    fun getBookCountInList(listId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM reading_lists")
    suspend fun getListCount(): Int
}
