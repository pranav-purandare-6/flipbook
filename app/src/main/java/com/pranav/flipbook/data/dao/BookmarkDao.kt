package com.pranav.flipbook.data.dao

import androidx.room.*
import com.pranav.flipbook.data.entity.BookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: BookmarkEntity): Long

    @Update
    suspend fun update(bookmark: BookmarkEntity)

    @Delete
    suspend fun delete(bookmark: BookmarkEntity)

    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY page ASC")
    fun getBookmarksForBook(bookId: Long): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks ORDER BY createdDate DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks ORDER BY createdDate DESC")
    suspend fun getAllBookmarksSnapshot(): List<BookmarkEntity>

    @Query("SELECT * FROM bookmarks WHERE id = :id")
    suspend fun getBookmarkById(id: Long): BookmarkEntity?

    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId AND page = :page LIMIT 1")
    suspend fun getBookmarkForPage(bookId: Long, page: Int): BookmarkEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE bookId = :bookId AND page = :page)")
    fun isPageBookmarked(bookId: Long, page: Int): Flow<Boolean>

    @Query("DELETE FROM bookmarks WHERE bookId = :bookId AND page = :page")
    suspend fun deleteBookmarkForPage(bookId: Long, page: Int)

    @Query("SELECT COUNT(*) FROM bookmarks WHERE bookId = :bookId")
    fun getBookmarkCountForBook(bookId: Long): Flow<Int>
}
