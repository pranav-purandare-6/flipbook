package com.pranav.flipbook.data.repository

import com.pranav.flipbook.data.dao.ReadingGoalDao
import com.pranav.flipbook.data.entity.ReadingGoalEntity
import kotlinx.coroutines.flow.Flow

class ReadingGoalRepository(private val goalDao: ReadingGoalDao) {

    fun getActiveGoals(): Flow<List<ReadingGoalEntity>> = goalDao.getActiveGoals()

    fun getAllGoals(): Flow<List<ReadingGoalEntity>> = goalDao.getAllGoals()

    suspend fun getGoalById(id: Long): ReadingGoalEntity? = goalDao.getGoalById(id)

    suspend fun getActiveGoalByType(type: String): ReadingGoalEntity? =
        goalDao.getActiveGoalByType(type)

    suspend fun insertGoal(goal: ReadingGoalEntity): Long = goalDao.insert(goal)

    suspend fun updateGoal(goal: ReadingGoalEntity) = goalDao.update(goal)

    suspend fun deleteGoal(goal: ReadingGoalEntity) = goalDao.delete(goal)
}
