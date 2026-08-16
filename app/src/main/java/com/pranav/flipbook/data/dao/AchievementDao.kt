package com.pranav.flipbook.data.dao

import androidx.room.*
import com.pranav.flipbook.data.entity.AchievementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(achievement: AchievementEntity): Long

    @Update
    suspend fun update(achievement: AchievementEntity)

    @Query("SELECT * FROM achievements ORDER BY isUnlocked DESC, progress DESC")
    fun getAllAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE isUnlocked = 1 ORDER BY unlockedDate DESC")
    fun getUnlockedAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE type = :type LIMIT 1")
    suspend fun getAchievementByType(type: String): AchievementEntity?

    @Query("UPDATE achievements SET progress = :progress WHERE type = :type")
    suspend fun updateProgress(type: String, progress: Int)

    @Query("UPDATE achievements SET isUnlocked = 1, unlockedDate = :date, progress = target WHERE type = :type")
    suspend fun unlockAchievement(type: String, date: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM achievements")
    suspend fun getCount(): Int
}
