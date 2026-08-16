package com.pranav.flipbook.data.repository

import com.pranav.flipbook.data.dao.AchievementDao
import com.pranav.flipbook.data.entity.AchievementEntity

class AchievementRepository(private val achievementDao: AchievementDao) {

    fun getAllAchievements() = achievementDao.getAllAchievements()

    fun getUnlockedAchievements() = achievementDao.getUnlockedAchievements()

    suspend fun getAchievementByType(type: String) = achievementDao.getAchievementByType(type)

    suspend fun insertAchievement(achievement: AchievementEntity) = achievementDao.insert(achievement)

    suspend fun updateProgress(type: String, progress: Int) = achievementDao.updateProgress(type, progress)

    suspend fun unlockAchievement(type: String) = achievementDao.unlockAchievement(type)

    suspend fun initializeAchievements() {
        if (achievementDao.getCount() > 0) return
        val defaults = listOf(
            AchievementEntity(type = "FIRST_BOOK", title = "First Book", description = "Open your first book", target = 1),
            AchievementEntity(type = "PAGES_100", title = "Century Reader", description = "Read 100 pages", target = 100),
            AchievementEntity(type = "PAGES_1000", title = "Bookworm", description = "Read 1,000 pages", target = 1000),
            AchievementEntity(type = "STREAK_7", title = "Week Warrior", description = "7-day reading streak", target = 7),
            AchievementEntity(type = "STREAK_30", title = "Monthly Master", description = "30-day reading streak", target = 30),
            AchievementEntity(type = "BOOKS_5", title = "Shelf Starter", description = "Add 5 books to library", target = 5),
            AchievementEntity(type = "BOOKS_10", title = "Library Builder", description = "Add 10 books to library", target = 10),
            AchievementEntity(type = "COMPLETE_1", title = "Finisher", description = "Complete your first book", target = 1),
            AchievementEntity(type = "COMPLETE_5", title = "Avid Reader", description = "Complete 5 books", target = 5),
            AchievementEntity(type = "NIGHT_READER", title = "Night Owl", description = "Read after 10 PM", target = 1),
            AchievementEntity(type = "WEEKEND_READER", title = "Weekend Reader", description = "Read on a weekend", target = 1),
            AchievementEntity(type = "BOOKMARK_10", title = "Page Marker", description = "Create 10 bookmarks", target = 10),
            AchievementEntity(type = "NOTE_5", title = "Note Taker", description = "Write 5 notes", target = 5),
        )
        defaults.forEach { achievementDao.insert(it) }
    }
}
