package com.pranav.flipbook.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reading_lists")
data class ReadingListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String? = null,
    val iconName: String = "list",
    val isSystemList: Boolean = false, // e.g. Currently Reading, Read Later, Completed
    val createdDate: Long = System.currentTimeMillis(),
    val modifiedDate: Long = System.currentTimeMillis()
)
