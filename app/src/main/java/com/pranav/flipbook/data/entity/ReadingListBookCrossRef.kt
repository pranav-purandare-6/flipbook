package com.pranav.flipbook.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "reading_list_book_cross_ref",
    primaryKeys = ["listId", "bookId"],
    foreignKeys = [
        ForeignKey(
            entity = ReadingListEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["listId"]),
        Index(value = ["bookId"])
    ]
)
data class ReadingListBookCrossRef(
    val listId: Long,
    val bookId: Long,
    val addedDate: Long = System.currentTimeMillis(),
    val displayOrder: Int = 0
)
