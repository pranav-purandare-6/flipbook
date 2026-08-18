package com.pranav.flipbook.ui.collections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pranav.flipbook.data.entity.BookEntity
import com.pranav.flipbook.data.entity.ReadingListEntity
import com.pranav.flipbook.ui.library.BookListItem
import com.pranav.flipbook.viewmodel.ReadingListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingListsScreen(
    onBack: () -> Unit,
    onBookClick: (Long) -> Unit,
    viewModel: ReadingListViewModel = viewModel()
) {
    val lists by viewModel.lists.collectAsState()
    val allBooks by viewModel.allBooks.collectAsState()
    var selectedListId by remember { mutableStateOf<Long?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showAddBookDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf<ReadingListEntity?>(null) }
    var showDeleteDialog by remember { mutableStateOf<ReadingListEntity?>(null) }
    var bookToRemove by remember { mutableStateOf<BookEntity?>(null) }
    val selectedList = lists.find { it.id == selectedListId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (selectedListId == null) "Reading Lists" else selectedList?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedListId != null) selectedListId = null else onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (selectedListId == null) {
                        IconButton(onClick = { showCreateDialog = true }) {
                            Icon(Icons.Default.Add, "Create list")
                        }
                    } else {
                        IconButton(onClick = { showAddBookDialog = true }) {
                            Icon(Icons.Default.Add, "Add book")
                        }
                        selectedList?.let { list ->
                            var expanded by remember { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { expanded = true }) {
                                    Icon(Icons.Default.MoreVert, "List actions")
                                }
                                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Rename") },
                                        onClick = {
                                            expanded = false
                                            showRenameDialog = list
                                        },
                                        enabled = !list.isSystemList
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete") },
                                        onClick = {
                                            expanded = false
                                            showDeleteDialog = list
                                        },
                                        enabled = !list.isSystemList
                                    )
                                }
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (selectedListId == null) {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(lists, key = { it.id }) { list ->
                    ReadingListCard(
                        list = list,
                        onClick = { selectedListId = list.id }
                    )
                }
            }
        } else {
            val books by viewModel.getBooksInList(selectedListId!!).collectAsState()
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (books.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No books in this list yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    items(books, key = { it.id }) { book ->
                        BookListItem(
                            book = book,
                            onClick = { onBookClick(book.id) },
                            onFavoriteClick = { viewModel.toggleFavorite(book) },
                            onRemoveClick = { bookToRemove = book }
                        )
                    }
                }
            }

            if (showAddBookDialog) {
                val existingIds = remember(books) { books.map { it.id }.toSet() }
                val availableBooks = allBooks.filterNot { it.id in existingIds }
                AlertDialog(
                    onDismissRequest = { showAddBookDialog = false },
                    title = { Text("Add Book") },
                    text = {
                        LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                            if (availableBooks.isEmpty()) {
                                item {
                                    Text(
                                        "All books are already in this list",
                                        modifier = Modifier.padding(12.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            items(availableBooks, key = { it.id }) { book ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.addBookToList(selectedListId!!, book.id)
                                            showAddBookDialog = false
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.MenuBook, null, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(book.displayTitle, maxLines = 1)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showAddBookDialog = false }) { Text("Done") }
                    }
                )
            }
        }
    }

    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Reading List") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("List name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) {
                        viewModel.createList(name.trim())
                        showCreateDialog = false
                    }
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") } }
        )
    }

    showRenameDialog?.let { list ->
        var name by remember(list.id) { mutableStateOf(list.name) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = { Text("Rename List") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("List name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.renameList(list, name)
                    showRenameDialog = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showRenameDialog = null }) { Text("Cancel") } }
        )
    }

    showDeleteDialog?.let { list ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete List") },
            text = { Text("Delete \"${list.name}\"? Books will stay in your library.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteList(list)
                    selectedListId = null
                    showDeleteDialog = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = null }) { Text("Cancel") } }
        )
    }

    bookToRemove?.let { book ->
        AlertDialog(
            onDismissRequest = { bookToRemove = null },
            title = { Text("Remove from List") },
            text = { Text("Remove \"${book.displayTitle}\" from this reading list?") },
            confirmButton = {
                TextButton(onClick = {
                    selectedListId?.let { viewModel.removeBookFromList(it, book.id) }
                    bookToRemove = null
                }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { bookToRemove = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ReadingListCard(list: ReadingListEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                when {
                    list.name.contains("Reading", ignoreCase = true) -> Icons.Default.MenuBook
                    list.name.contains("Later", ignoreCase = true) -> Icons.Default.Schedule
                    list.name.contains("Completed", ignoreCase = true) -> Icons.Default.CheckCircle
                    else -> Icons.Default.List
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(list.name, style = MaterialTheme.typography.titleMedium)
                list.description?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
