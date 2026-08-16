package com.pranav.flipbook.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,
    val title: String,
    val description: String,
    val isUnlocked: Boolean = false,
    val unlockedDate: Long? = null,
    val progress: Int = 0,
    val target: Int = 1
)
