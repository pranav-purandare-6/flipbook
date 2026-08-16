package com.pranav.flipbook.ui.notes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pranav.flipbook.data.entity.BookmarkEntity
import com.pranav.flipbook.data.entity.HighlightEntity
import com.pranav.flipbook.data.entity.NoteEntity
import com.pranav.flipbook.utils.formatDate
import com.pranav.flipbook.viewmodel.NotesViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesHighlightsScreen(
    bookId: Long,
    onBack: () -> Unit,
    onNavigateToPage: (Long, Int) -> Unit,
    viewModel: NotesViewModel = viewModel()
) {
    val notes by viewModel.getNotesForBook(bookId).collectAsState(initial = emptyList())
    val highlights by viewModel.getHighlightsForBook(bookId).collectAsState(initial = emptyList())
    val bookmarks by viewModel.getBookmarksForBook(bookId).collectAsState(initial = emptyList())

    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    val tabs = listOf("Notes", "Highlights", "Bookmarks")

    var showAddNote by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<NoteEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notes & Highlights") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }
            )
        },
        floatingActionButton = {
            if (pagerState.currentPage == 0) {
                FloatingActionButton(onClick = { showAddNote = true }) {
                    Icon(Icons.Default.Add, "Add Note")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = pagerState.currentPage) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        text = {
                            val count = when (index) {
                                0 -> notes.size; 1 -> highlights.size; else -> bookmarks.size
                            }
                            Text("$title ($count)")
                        }
                    )
                }
            }

            HorizontalPager(state = pagerState) { page ->
                when (page) {
                    0 -> NotesTab(notes, onNavigateToPage, viewModel, bookId, onEdit = { editingNote = it })
                    1 -> HighlightsTab(highlights, onNavigateToPage, viewModel)
                    2 -> BookmarksTab(bookmarks, onNavigateToPage, viewModel)
                }
            }
        }

        // Add/Edit note dialog
        if (showAddNote || editingNote != null) {
            val note = editingNote
            var text by remember { mutableStateOf(note?.text ?: "") }
            var pageNum by remember { mutableStateOf(note?.page?.plus(1)?.toString() ?: "1") }

            AlertDialog(
                onDismissRequest = { showAddNote = false; editingNote = null },
                title = { Text(if (note != null) "Edit Note" else "Add Note") },
                text = {
                    Column {
                        if (note == null) {
                            OutlinedTextField(
                                value = pageNum,
                                onValueChange = { pageNum = it.filter { c -> c.isDigit() } },
                                label = { Text("Page") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it },
                            label = { Text("Note") },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                            maxLines = 10
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (text.isNotBlank()) {
                            if (note != null) {
                                viewModel.updateNote(note.copy(text = text))
                            } else {
                                val page = (pageNum.toIntOrNull() ?: 1) - 1
                                viewModel.createNote(bookId, page.coerceAtLeast(0), text)
                            }
                        }
                        showAddNote = false; editingNote = null
                    }) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddNote = false; editingNote = null }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
private fun NotesTab(
    notes: List<NoteEntity>,
    onNavigateToPage: (Long, Int) -> Unit,
    viewModel: NotesViewModel,
    bookId: Long,
    onEdit: (NoteEntity) -> Unit
) {
    if (notes.isEmpty()) {
        EmptyTabContent("No notes yet", "Tap + to add your first note", Icons.Outlined.StickyNote2)
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(notes, key = { it.id }) { note ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onNavigateToPage(note.bookId, note.page) },
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Page ${note.page + 1}", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.weight(1f))
                            Text(note.modifiedDate.formatDate(), style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(note.text, style = MaterialTheme.typography.bodyMedium, maxLines = 4)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            TextButton(onClick = { onEdit(note) }, contentPadding = PaddingValues(0.dp)) {
                                Text("Edit", style = MaterialTheme.typography.labelSmall)
                            }
                            TextButton(onClick = { viewModel.deleteNote(note) }, contentPadding = PaddingValues(0.dp)) {
                                Text("Delete", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HighlightsTab(
    highlights: List<HighlightEntity>,
    onNavigateToPage: (Long, Int) -> Unit,
    viewModel: NotesViewModel
) {
    if (highlights.isEmpty()) {
        EmptyTabContent("No highlights yet", "Select text while reading to highlight", Icons.Outlined.Highlight)
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(highlights, key = { it.id }) { highlight ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onNavigateToPage(highlight.bookId, highlight.page) },
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Page ${highlight.page + 1}", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary)
                            Text("\"${highlight.text}\"", style = MaterialTheme.typography.bodyMedium, maxLines = 3)
                        }
                        IconButton(onClick = { viewModel.deleteHighlight(highlight) }) {
                            Icon(Icons.Default.Delete, "Delete", modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookmarksTab(
    bookmarks: List<BookmarkEntity>,
    onNavigateToPage: (Long, Int) -> Unit,
    viewModel: NotesViewModel
) {
    if (bookmarks.isEmpty()) {
        EmptyTabContent("No bookmarks yet", "Tap the bookmark icon while reading", Icons.Outlined.BookmarkBorder)
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(bookmarks, key = { it.id }) { bookmark ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onNavigateToPage(bookmark.bookId, bookmark.page) },
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Bookmark, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(bookmark.title ?: "Page ${bookmark.page + 1}", style = MaterialTheme.typography.titleSmall)
                            Text(bookmark.createdDate.formatDate(), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { viewModel.deleteBookmark(bookmark) }) {
                            Icon(Icons.Default.Delete, "Delete", modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyTabContent(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
