package com.pranav.flipbook.data.dao

import androidx.room.*
import com.pranav.flipbook.data.entity.FavoriteQuoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteQuoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(quote: FavoriteQuoteEntity): Long

    @Update
    suspend fun update(quote: FavoriteQuoteEntity)

    @Delete
    suspend fun delete(quote: FavoriteQuoteEntity)

    @Query("SELECT * FROM favorite_quotes ORDER BY createdDate DESC")
    fun getAllQuotes(): Flow<List<FavoriteQuoteEntity>>

    @Query("SELECT * FROM favorite_quotes ORDER BY createdDate DESC")
    suspend fun getAllQuotesSnapshot(): List<FavoriteQuoteEntity>

    @Query("SELECT * FROM favorite_quotes WHERE bookId = :bookId ORDER BY page ASC")
    fun getQuotesForBook(bookId: Long): Flow<List<FavoriteQuoteEntity>>

    @Query("SELECT * FROM favorite_quotes WHERE id = :id")
    suspend fun getQuoteById(id: Long): FavoriteQuoteEntity?

    @Query("SELECT * FROM favorite_quotes WHERE text LIKE '%' || :query || '%' ORDER BY createdDate DESC")
    fun searchQuotes(query: String): Flow<List<FavoriteQuoteEntity>>
}
