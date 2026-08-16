package com.pranav.flipbook.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.pranav.flipbook.data.dao.*
import com.pranav.flipbook.data.entity.*

@Database(
    entities = [
        BookEntity::class,
        BookmarkEntity::class,
        NoteEntity::class,
        HighlightEntity::class,
        CollectionEntity::class,
        BookCollectionCrossRef::class,
        ReadingSessionEntity::class,
        ReadingGoalEntity::class,
        AchievementEntity::class,
        FavoriteQuoteEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class FlipBookDatabase : RoomDatabase() {

    abstract fun bookDao(): BookDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun noteDao(): NoteDao
    abstract fun highlightDao(): HighlightDao
    abstract fun collectionDao(): CollectionDao
    abstract fun readingSessionDao(): ReadingSessionDao
    abstract fun readingGoalDao(): ReadingGoalDao
    abstract fun achievementDao(): AchievementDao
    abstract fun favoriteQuoteDao(): FavoriteQuoteDao

    companion object {
        @Volatile
        private var INSTANCE: FlipBookDatabase? = null

        fun getInstance(context: Context): FlipBookDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FlipBookDatabase::class.java,
                    "flipbook_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
