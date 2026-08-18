package com.pranav.flipbook.data.dao

import androidx.room.*
import com.pranav.flipbook.data.entity.BookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(book: BookEntity): Long

    @Update
    suspend fun update(book: BookEntity)

    @Delete
    suspend fun delete(book: BookEntity)

    @Query("SELECT * FROM books ORDER BY dateAdded DESC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books ORDER BY dateAdded DESC")
    suspend fun getAllBooksSnapshot(): List<BookEntity>

    @Query("SELECT * FROM books WHERE id = :bookId")
    suspend fun getBookById(bookId: Long): BookEntity?

    @Query("SELECT * FROM books WHERE id = :bookId")
    fun getBookByIdFlow(bookId: Long): Flow<BookEntity?>

    @Query("SELECT * FROM books WHERE uri = :uri LIMIT 1")
    suspend fun getBookByUri(uri: String): BookEntity?

    @Query("SELECT * FROM books WHERE isFavorite = 1 ORDER BY lastOpened DESC")
    fun getFavoriteBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books ORDER BY COALESCE(lastOpened, dateAdded) DESC LIMIT :limit")
    fun getRecentlyOpened(limit: Int = 1000): Flow<List<BookEntity>>

    @Query("SELECT * FROM books ORDER BY dateAdded DESC LIMIT :limit")
    fun getRecentlyAdded(limit: Int = 10): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE lastOpened IS NOT NULL AND currentPage > 0 ORDER BY lastOpened DESC LIMIT :limit")
    fun getContinueReading(limit: Int = 5): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE title LIKE '%' || :query || '%' OR fileName LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%' OR customTitle LIKE '%' || :query || '%' ORDER BY lastOpened DESC")
    fun searchBooks(query: String): Flow<List<BookEntity>>

    @Query("UPDATE books SET currentPage = :page, readingProgress = :progress, lastOpened = :timestamp WHERE id = :bookId")
    suspend fun updateProgress(bookId: Long, page: Int, progress: Float, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE books SET isFavorite = :isFavorite WHERE id = :bookId")
    suspend fun updateFavorite(bookId: Long, isFavorite: Boolean)

    @Query("UPDATE books SET customTitle = :title WHERE id = :bookId")
    suspend fun updateCustomTitle(bookId: Long, title: String?)

    @Query("UPDATE books SET coverPath = :path WHERE id = :bookId")
    suspend fun updateCoverPath(bookId: Long, path: String)

    @Query("UPDATE books SET pageCount = :count WHERE id = :bookId")
    suspend fun updatePageCount(bookId: Long, count: Int)

    @Query("UPDATE books SET totalReadingTime = totalReadingTime + :duration, totalPagesRead = totalPagesRead + :pages WHERE id = :bookId")
    suspend fun addReadingStats(bookId: Long, duration: Long, pages: Int)

    @Query("UPDATE books SET isCompleted = :completed WHERE id = :bookId")
    suspend fun updateCompleted(bookId: Long, completed: Boolean)

    @Query("SELECT COUNT(*) FROM books")
    fun getBookCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM books WHERE isCompleted = 1")
    fun getCompletedBookCount(): Flow<Int>

    @Query("SELECT SUM(totalPagesRead) FROM books")
    fun getTotalPagesRead(): Flow<Int?>

    @Query("SELECT SUM(totalReadingTime) FROM books")
    fun getTotalReadingTime(): Flow<Long?>

    // Sorting queries
    @Query("SELECT * FROM books ORDER BY COALESCE(customTitle, title) ASC")
    fun getBooksSortedByNameAsc(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books ORDER BY COALESCE(customTitle, title) DESC")
    fun getBooksSortedByNameDesc(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books ORDER BY fileSize DESC")
    fun getBooksSortedBySize(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books ORDER BY readingProgress DESC")
    fun getBooksSortedByProgress(): Flow<List<BookEntity>>
}
