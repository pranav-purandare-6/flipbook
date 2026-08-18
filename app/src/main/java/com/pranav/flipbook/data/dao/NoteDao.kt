package com.pranav.flipbook.data.dao

import androidx.room.*
import com.pranav.flipbook.data.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity): Long

    @Update
    suspend fun update(note: NoteEntity)

    @Delete
    suspend fun delete(note: NoteEntity)

    @Query("SELECT * FROM notes WHERE bookId = :bookId ORDER BY page ASC")
    fun getNotesForBook(bookId: Long): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes ORDER BY modifiedDate DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes ORDER BY modifiedDate DESC")
    suspend fun getAllNotesSnapshot(): List<NoteEntity>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): NoteEntity?

    @Query("SELECT * FROM notes WHERE bookId = :bookId AND page = :page ORDER BY createdDate DESC")
    fun getNotesForPage(bookId: Long, page: Int): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE text LIKE '%' || :query || '%' ORDER BY modifiedDate DESC")
    fun searchNotes(query: String): Flow<List<NoteEntity>>

    @Query("SELECT COUNT(*) FROM notes WHERE bookId = :bookId")
    fun getNoteCountForBook(bookId: Long): Flow<Int>
}
