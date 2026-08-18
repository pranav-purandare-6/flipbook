package com.pranav.flipbook.data.dao

import androidx.room.*
import com.pranav.flipbook.data.entity.HighlightEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HighlightDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(highlight: HighlightEntity): Long

    @Update
    suspend fun update(highlight: HighlightEntity)

    @Delete
    suspend fun delete(highlight: HighlightEntity)

    @Query("SELECT * FROM highlights WHERE bookId = :bookId ORDER BY page ASC, startOffset ASC")
    fun getHighlightsForBook(bookId: Long): Flow<List<HighlightEntity>>

    @Query("SELECT * FROM highlights ORDER BY createdDate DESC")
    fun getAllHighlights(): Flow<List<HighlightEntity>>

    @Query("SELECT * FROM highlights ORDER BY createdDate DESC")
    suspend fun getAllHighlightsSnapshot(): List<HighlightEntity>

    @Query("SELECT * FROM highlights WHERE bookId = :bookId AND page = :page ORDER BY startOffset ASC")
    fun getHighlightsForPage(bookId: Long, page: Int): Flow<List<HighlightEntity>>

    @Query("SELECT * FROM highlights WHERE id = :id")
    suspend fun getHighlightById(id: Long): HighlightEntity?

    @Query("SELECT COUNT(*) FROM highlights WHERE bookId = :bookId")
    fun getHighlightCountForBook(bookId: Long): Flow<Int>

    @Query("SELECT page, COUNT(*) as count FROM highlights WHERE bookId = :bookId GROUP BY page ORDER BY count DESC LIMIT :limit")
    suspend fun getMostHighlightedPages(bookId: Long, limit: Int = 5): List<PageHighlightCount>
}

data class PageHighlightCount(
    val page: Int,
    val count: Int
)
