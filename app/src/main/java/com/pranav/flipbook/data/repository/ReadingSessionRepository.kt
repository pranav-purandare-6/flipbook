package com.pranav.flipbook.data.repository

import com.pranav.flipbook.data.dao.ReadingSessionDao
import com.pranav.flipbook.data.entity.ReadingSessionEntity
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class ReadingSessionRepository(private val sessionDao: ReadingSessionDao) {

    fun getSessionsForBook(bookId: Long): Flow<List<ReadingSessionEntity>> =
        sessionDao.getSessionsForBook(bookId)

    fun getAllSessions(): Flow<List<ReadingSessionEntity>> = sessionDao.getAllSessions()

    fun getRecentSessions(limit: Int = 50): Flow<List<ReadingSessionEntity>> =
        sessionDao.getRecentSessions(limit)

    fun getTotalPagesRead(): Flow<Int?> = sessionDao.getTotalPagesRead()

    fun getTotalReadingTime(): Flow<Long?> = sessionDao.getTotalReadingTime()

    suspend fun insertSession(session: ReadingSessionEntity): Long = sessionDao.insert(session)

    suspend fun updateSession(session: ReadingSessionEntity) = sessionDao.update(session)

    suspend fun getPagesReadToday(): Int {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis
        val endOfDay = startOfDay + 86400000L
        return sessionDao.getPagesReadOnDay(startOfDay, endOfDay) ?: 0
    }

    suspend fun getReadingTimeToday(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis
        val endOfDay = startOfDay + 86400000L
        return sessionDao.getReadingTimeOnDay(startOfDay, endOfDay) ?: 0L
    }

    suspend fun getPagesReadThisWeek(): Int {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return sessionDao.getPagesReadSince(cal.timeInMillis) ?: 0
    }

    suspend fun getPagesReadThisMonth(): Int {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return sessionDao.getPagesReadSince(cal.timeInMillis) ?: 0
    }

    suspend fun getCurrentStreak(): Int {
        val readingDays = sessionDao.getReadingDays()
        if (readingDays.isEmpty()) return 0

        var streak = 0
        val todayDayStamp = System.currentTimeMillis() / 86400000L
        var expectedDay = todayDayStamp

        for (dayStamp in readingDays) {
            if (dayStamp == expectedDay || dayStamp == expectedDay - 1) {
                streak++
                expectedDay = dayStamp - 1
            } else if (dayStamp < expectedDay - 1) {
                break
            }
        }
        return streak
    }

    suspend fun getLongestStreak(): Int {
        val readingDays = sessionDao.getReadingDays().sorted()
        if (readingDays.isEmpty()) return 0

        var longest = 1
        var current = 1
        for (i in 1 until readingDays.size) {
            if (readingDays[i] == readingDays[i - 1] + 1) {
                current++
                if (current > longest) longest = current
            } else if (readingDays[i] != readingDays[i - 1]) {
                current = 1
            }
        }
        return longest
    }

    suspend fun getReadingDays(): List<Long> = sessionDao.getReadingDays()

    suspend fun getSessionsBetween(startTime: Long, endTime: Long): List<ReadingSessionEntity> =
        sessionDao.getSessionsBetween(startTime, endTime)
}
