package com.pranav.flipbook.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reading_goals")
data class ReadingGoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // DAILY_PAGES, DAILY_TIME, WEEKLY_PAGES, MONTHLY_BOOKS
    val target: Int,
    val createdDate: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)
