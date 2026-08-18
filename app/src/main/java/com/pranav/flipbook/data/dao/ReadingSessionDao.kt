package com.pranav.flipbook.data.dao

import androidx.room.*
import com.pranav.flipbook.data.entity.ReadingSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: ReadingSessionEntity): Long

    @Update
    suspend fun update(session: ReadingSessionEntity)

    @Delete
    suspend fun delete(session: ReadingSessionEntity)

    @Query("SELECT * FROM reading_sessions WHERE bookId = :bookId ORDER BY startTime DESC")
    fun getSessionsForBook(bookId: Long): Flow<List<ReadingSessionEntity>>

    @Query("SELECT * FROM reading_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<ReadingSessionEntity>>

    @Query("SELECT * FROM reading_sessions ORDER BY startTime DESC")
    suspend fun getAllSessionsSnapshot(): List<ReadingSessionEntity>

    @Query("SELECT * FROM reading_sessions ORDER BY startTime DESC LIMIT :limit")
    fun getRecentSessions(limit: Int = 50): Flow<List<ReadingSessionEntity>>

    @Query("SELECT * FROM reading_sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): ReadingSessionEntity?

    @Query("SELECT SUM(pagesRead) FROM reading_sessions WHERE startTime >= :startOfDay AND startTime < :endOfDay")
    suspend fun getPagesReadOnDay(startOfDay: Long, endOfDay: Long): Int?

    @Query("SELECT SUM(duration) FROM reading_sessions WHERE startTime >= :startOfDay AND startTime < :endOfDay")
    suspend fun getReadingTimeOnDay(startOfDay: Long, endOfDay: Long): Long?

    @Query("SELECT SUM(pagesRead) FROM reading_sessions WHERE startTime >= :startTime")
    suspend fun getPagesReadSince(startTime: Long): Int?

    @Query("SELECT SUM(duration) FROM reading_sessions WHERE startTime >= :startTime")
    suspend fun getReadingTimeSince(startTime: Long): Long?

    @Query("SELECT SUM(pagesRead) FROM reading_sessions")
    fun getTotalPagesRead(): Flow<Int?>

    @Query("SELECT SUM(duration) FROM reading_sessions")
    fun getTotalReadingTime(): Flow<Long?>

    @Query("""
        SELECT DISTINCT startTime / 86400000 as dayTimestamp 
        FROM reading_sessions 
        WHERE pagesRead > 0 
        ORDER BY dayTimestamp DESC
    """)
    suspend fun getReadingDays(): List<Long>

    @Query("SELECT * FROM reading_sessions WHERE startTime >= :startTime AND startTime < :endTime ORDER BY startTime ASC")
    suspend fun getSessionsBetween(startTime: Long, endTime: Long): List<ReadingSessionEntity>
}
