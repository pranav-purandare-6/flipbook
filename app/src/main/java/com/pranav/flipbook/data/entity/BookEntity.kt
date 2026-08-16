package com.pranav.flipbook.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uri: String,
    val title: String,
    val customTitle: String? = null,
    val author: String? = null,
    val subject: String? = null,
    val creator: String? = null,
    val producer: String? = null,
    val fileName: String,
    val fileSize: Long = 0,
    val pageCount: Int = 0,
    val currentPage: Int = 0,
    val readingProgress: Float = 0f,
    val coverPath: String? = null,
    val isFavorite: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis(),
    val lastOpened: Long? = null,
    val totalReadingTime: Long = 0L,
    val totalPagesRead: Int = 0,
    val isCompleted: Boolean = false
) {
    val displayTitle: String
        get() = customTitle ?: title
}
