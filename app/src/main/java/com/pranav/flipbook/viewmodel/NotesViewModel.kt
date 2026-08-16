package com.pranav.flipbook.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pranav.flipbook.FlipBookApplication
import com.pranav.flipbook.data.entity.BookmarkEntity
import com.pranav.flipbook.data.entity.HighlightEntity
import com.pranav.flipbook.data.entity.NoteEntity
import com.pranav.flipbook.data.repository.BookmarkRepository
import com.pranav.flipbook.data.repository.HighlightRepository
import com.pranav.flipbook.data.repository.NoteRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NotesViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as FlipBookApplication).database
    private val noteRepo = NoteRepository(db.noteDao())
    private val highlightRepo = HighlightRepository(db.highlightDao())
    private val bookmarkRepo = BookmarkRepository(db.bookmarkDao())

    fun getNotesForBook(bookId: Long): Flow<List<NoteEntity>> = noteRepo.getNotesForBook(bookId)

    fun getAllNotes(): Flow<List<NoteEntity>> = noteRepo.getAllNotes()

    fun getHighlightsForBook(bookId: Long): Flow<List<HighlightEntity>> =
        highlightRepo.getHighlightsForBook(bookId)

    fun getAllHighlights(): Flow<List<HighlightEntity>> = highlightRepo.getAllHighlights()

    fun getBookmarksForBook(bookId: Long): Flow<List<BookmarkEntity>> =
        bookmarkRepo.getBookmarksForBook(bookId)

    fun getAllBookmarks(): Flow<List<BookmarkEntity>> = bookmarkRepo.getAllBookmarks()

    fun createNote(bookId: Long, page: Int, text: String) {
        viewModelScope.launch {
            noteRepo.insertNote(NoteEntity(bookId = bookId, page = page, text = text))
        }
    }

    fun updateNote(note: NoteEntity) {
        viewModelScope.launch {
            noteRepo.updateNote(note.copy(modifiedDate = System.currentTimeMillis()))
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch { noteRepo.deleteNote(note) }
    }

    fun createHighlight(bookId: Long, page: Int, text: String, color: Int) {
        viewModelScope.launch {
            highlightRepo.insertHighlight(
                HighlightEntity(bookId = bookId, page = page, text = text, color = color)
            )
        }
    }

    fun deleteHighlight(highlight: HighlightEntity) {
        viewModelScope.launch { highlightRepo.deleteHighlight(highlight) }
    }

    fun updateBookmark(bookmark: BookmarkEntity) {
        viewModelScope.launch { bookmarkRepo.updateBookmark(bookmark) }
    }

    fun deleteBookmark(bookmark: BookmarkEntity) {
        viewModelScope.launch { bookmarkRepo.deleteBookmark(bookmark) }
    }

    fun searchNotes(query: String): Flow<List<NoteEntity>> = noteRepo.searchNotes(query)

    fun exportNotesAndHighlights(bookId: Long): Flow<String> = flow {
        val notes = noteRepo.getNotesForBook(bookId).first()
        val highlights = highlightRepo.getHighlightsForBook(bookId).first()
        val bookmarks = bookmarkRepo.getBookmarksForBook(bookId).first()

        val sb = StringBuilder()
        sb.appendLine("# Notes & Highlights")
        sb.appendLine()

        if (bookmarks.isNotEmpty()) {
            sb.appendLine("## Bookmarks")
            bookmarks.forEach { bm ->
                sb.appendLine("- Page ${bm.page + 1}${bm.title?.let { ": $it" } ?: ""}")
            }
            sb.appendLine()
        }

        if (notes.isNotEmpty()) {
            sb.appendLine("## Notes")
            notes.forEach { note ->
                sb.appendLine("### Page ${note.page + 1}")
                sb.appendLine(note.text)
                sb.appendLine()
            }
        }

        if (highlights.isNotEmpty()) {
            sb.appendLine("## Highlights")
            highlights.forEach { hl ->
                sb.appendLine("- Page ${hl.page + 1}: \"${hl.text}\"")
            }
        }

        emit(sb.toString())
    }
}
