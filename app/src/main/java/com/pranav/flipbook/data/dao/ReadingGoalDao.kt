package com.pranav.flipbook.data.dao

import androidx.room.*
import com.pranav.flipbook.data.entity.ReadingGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingGoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: ReadingGoalEntity): Long

    @Update
    suspend fun update(goal: ReadingGoalEntity)

    @Delete
    suspend fun delete(goal: ReadingGoalEntity)

    @Query("SELECT * FROM reading_goals WHERE isActive = 1 ORDER BY createdDate DESC")
    fun getActiveGoals(): Flow<List<ReadingGoalEntity>>

    @Query("SELECT * FROM reading_goals ORDER BY createdDate DESC")
    fun getAllGoals(): Flow<List<ReadingGoalEntity>>

    @Query("SELECT * FROM reading_goals WHERE id = :id")
    suspend fun getGoalById(id: Long): ReadingGoalEntity?

    @Query("SELECT * FROM reading_goals WHERE type = :type AND isActive = 1 LIMIT 1")
    suspend fun getActiveGoalByType(type: String): ReadingGoalEntity?
}
