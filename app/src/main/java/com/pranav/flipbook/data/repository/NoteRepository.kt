package com.pranav.flipbook.data.repository

import com.pranav.flipbook.data.dao.NoteDao
import com.pranav.flipbook.data.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {

    fun getNotesForBook(bookId: Long): Flow<List<NoteEntity>> = noteDao.getNotesForBook(bookId)

    fun getAllNotes(): Flow<List<NoteEntity>> = noteDao.getAllNotes()

    fun getNotesForPage(bookId: Long, page: Int): Flow<List<NoteEntity>> =
        noteDao.getNotesForPage(bookId, page)

    fun searchNotes(query: String): Flow<List<NoteEntity>> = noteDao.searchNotes(query)

    fun getNoteCountForBook(bookId: Long): Flow<Int> = noteDao.getNoteCountForBook(bookId)

    suspend fun getNoteById(id: Long): NoteEntity? = noteDao.getNoteById(id)

    suspend fun insertNote(note: NoteEntity): Long = noteDao.insert(note)

    suspend fun updateNote(note: NoteEntity) = noteDao.update(note)

    suspend fun deleteNote(note: NoteEntity) = noteDao.delete(note)
}
