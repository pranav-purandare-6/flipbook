package com.pranav.flipbook.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
        FavoriteQuoteEntity::class,
        ReadingListEntity::class,
        ReadingListBookCrossRef::class
    ],
    version = 2,
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
    abstract fun readingListDao(): ReadingListDao

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
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `reading_lists` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT,
                        `iconName` TEXT NOT NULL,
                        `isSystemList` INTEGER NOT NULL,
                        `createdDate` INTEGER NOT NULL,
                        `modifiedDate` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `reading_list_book_cross_ref` (
                        `listId` INTEGER NOT NULL,
                        `bookId` INTEGER NOT NULL,
                        `addedDate` INTEGER NOT NULL,
                        `displayOrder` INTEGER NOT NULL,
                        PRIMARY KEY(`listId`, `bookId`),
                        FOREIGN KEY(`listId`) REFERENCES `reading_lists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`bookId`) REFERENCES `books`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_reading_list_book_cross_ref_listId` ON `reading_list_book_cross_ref` (`listId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_reading_list_book_cross_ref_bookId` ON `reading_list_book_cross_ref` (`bookId`)")
            }
        }
    }
}
