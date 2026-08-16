package com.pranav.flipbook.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pranav.flipbook.FlipBookApplication
import com.pranav.flipbook.data.entity.AchievementEntity
import com.pranav.flipbook.data.entity.ReadingGoalEntity
import com.pranav.flipbook.data.entity.ReadingSessionEntity
import com.pranav.flipbook.data.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class StatisticsState(
    val totalBooksOpened: Int = 0,
    val booksCompleted: Int = 0,
    val totalPagesRead: Int = 0,
    val totalReadingTime: Long = 0L,
    val pagesReadToday: Int = 0,
    val pagesReadThisWeek: Int = 0,
    val pagesReadThisMonth: Int = 0,
    val readingTimeToday: Long = 0L,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val isLoading: Boolean = true
)

class StatisticsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as FlipBookApplication).database
    private val bookRepo = BookRepository(db.bookDao())
    private val sessionRepo = ReadingSessionRepository(db.readingSessionDao())
    private val goalRepo = ReadingGoalRepository(db.readingGoalDao())
    private val achievementRepo = AchievementRepository(db.achievementDao())

    private val _stats = MutableStateFlow(StatisticsState())
    val stats: StateFlow<StatisticsState> = _stats.asStateFlow()

    val recentSessions: StateFlow<List<ReadingSessionEntity>> = sessionRepo.getRecentSessions(20)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val activeGoals: StateFlow<List<ReadingGoalEntity>> = goalRepo.getActiveGoals()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val achievements: StateFlow<List<AchievementEntity>> = achievementRepo.getAllAchievements()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        loadStatistics()
    }

    private fun loadStatistics() {
        viewModelScope.launch {
            combine(
                bookRepo.getBookCount(),
                bookRepo.getCompletedBookCount(),
                sessionRepo.getTotalPagesRead(),
                sessionRepo.getTotalReadingTime()
            ) { bookCount, completed, pages, time ->
                StatisticsState(
                    totalBooksOpened = bookCount,
                    booksCompleted = completed,
                    totalPagesRead = pages ?: 0,
                    totalReadingTime = time ?: 0L
                )
            }.collect { baseStats ->
                val pToday = sessionRepo.getPagesReadToday()
                val pWeek = sessionRepo.getPagesReadThisWeek()
                val pMonth = sessionRepo.getPagesReadThisMonth()
                val tToday = sessionRepo.getReadingTimeToday()
                val cStreak = sessionRepo.getCurrentStreak()
                val lStreak = sessionRepo.getLongestStreak()

                _stats.value = baseStats.copy(
                    pagesReadToday = pToday,
                    pagesReadThisWeek = pWeek,
                    pagesReadThisMonth = pMonth,
                    readingTimeToday = tToday,
                    currentStreak = cStreak,
                    longestStreak = lStreak,
                    isLoading = false
                )
            }
        }
    }

    fun createGoal(type: String, target: Int) {
        viewModelScope.launch {
            goalRepo.insertGoal(
                ReadingGoalEntity(type = type, target = target)
            )
        }
    }

    fun deleteGoal(goal: ReadingGoalEntity) {
        viewModelScope.launch {
            goalRepo.deleteGoal(goal)
        }
    }
}
