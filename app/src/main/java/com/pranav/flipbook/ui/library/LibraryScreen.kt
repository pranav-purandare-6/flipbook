package com.pranav.flipbook.ui.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pranav.flipbook.data.entity.BookEntity
import com.pranav.flipbook.viewmodel.LibraryLayout
import com.pranav.flipbook.viewmodel.LibraryViewModel
import com.pranav.flipbook.viewmodel.SortOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onBookClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    onStatisticsClick: () -> Unit,
    onCollectionsClick: () -> Unit,
    onReadingListsClick: () -> Unit,
    onFavoriteQuotesClick: () -> Unit,
    onBookInfoClick: (Long) -> Unit,
    viewModel: LibraryViewModel = viewModel()
) {
    val allBooks by viewModel.allBooks.collectAsState()
    val continueReading by viewModel.continueReading.collectAsState()
    val favorites by viewModel.favoriteBooks.collectAsState()
    val bookCount by viewModel.bookCount.collectAsState()
    val layout by viewModel.layout.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()
    val importError by viewModel.importError.collectAsState()

    var showSortMenu by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<BookEntity?>(null) }
    var showRenameDialog by remember { mutableStateOf<BookEntity?>(null) }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importPdf(it) }
    }

    Scaffold(
        topBar = {
            if (isSearchActive) {
                SearchBar(
                    inputField = {
                        SearchBarDefaults.InputField(
                            query = searchQuery,
                            onQueryChange = { viewModel.setSearchQuery(it) },
                            onSearch = { },
                            expanded = false,
                            onExpandedChange = { },
                            placeholder = { Text("Search books...") },
                            leadingIcon = {
                                IconButton(onClick = {
                                    isSearchActive = false
                                    viewModel.setSearchQuery("")
                                }) {
                                    Icon(Icons.Default.ArrowBack, "Close search")
                                }
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(Icons.Default.Clear, "Clear")
                                    }
                                }
                            }
                        )
                    },
                    expanded = false,
                    onExpandedChange = { },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                ) { }
            } else {
                TopAppBar(
                    title = {
                        Text(
                            "Flip Book",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    },
                    actions = {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, "Search")
                        }
                        IconButton(onClick = {
                            viewModel.setLayout(
                                when (layout) {
                                    LibraryLayout.GRID -> LibraryLayout.LIST
                                    LibraryLayout.LIST -> LibraryLayout.BOOKSHELF
                                    LibraryLayout.BOOKSHELF -> LibraryLayout.GRID
                                }
                            )
                        }) {
                            Icon(
                                when (layout) {
                                    LibraryLayout.GRID -> Icons.Default.ViewList
                                    LibraryLayout.LIST -> Icons.Default.TableRows
                                    LibraryLayout.BOOKSHELF -> Icons.Default.GridView
                                },
                                "Toggle layout"
                            )
                        }
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.Default.Sort, "Sort")
                            }
                            DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                                SortOrder.entries.forEach { order ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(when(order) {
                                                SortOrder.RECENTLY_OPENED -> "Recently Opened"
                                                SortOrder.NAME_ASC -> "Name A-Z"
                                                SortOrder.NAME_DESC -> "Name Z-A"
                                                SortOrder.DATE_ADDED -> "Date Added"
                                                SortOrder.FILE_SIZE -> "File Size"
                                                SortOrder.PROGRESS -> "Reading Progress"
                                            })
                                        },
                                        onClick = {
                                            viewModel.setSortOrder(order)
                                            showSortMenu = false
                                        },
                                        leadingIcon = {
                                            if (sortOrder == order) Icon(Icons.Default.Check, null)
                                        }
                                    )
                                }
                            }
                        }
                        IconButton(onClick = onStatisticsClick) {
                            Icon(Icons.Outlined.BarChart, "Statistics")
                        }
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Outlined.Settings, "Settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { pdfLauncher.launch(arrayOf("application/pdf")) },
                icon = { Icon(Icons.Default.Add, "Import") },
                text = { Text("Import PDF") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
        if (bookCount == 0 && !isImporting) {
            // Empty state
            EmptyLibraryState(
                onImport = { pdfLauncher.launch(arrayOf("application/pdf")) },
                modifier = Modifier.padding(padding).fillMaxSize()
            )
        } else if (isSearchActive && searchQuery.isNotEmpty()) {
            // Search results
            BookListContent(
                books = allBooks,
                layout = layout,
                onBookClick = onBookClick,
                onFavoriteClick = { book -> viewModel.toggleFavorite(book.id, book.isFavorite) },
                onBookInfoClick = onBookInfoClick,
                onRenameClick = { book -> showRenameDialog = book },
                onDeleteClick = { book -> showDeleteDialog = book },
                modifier = Modifier.padding(padding)
            )
        } else {
            // Main library view
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // Continue Reading
                if (continueReading.isNotEmpty()) {
                    item {
                        SectionHeader("Continue Reading")
                    }
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            items(continueReading, key = { it.id }) { book ->
                                BookCard(
                                    book = book,
                                    onClick = { onBookClick(book.id) },
                                    onFavoriteClick = { viewModel.toggleFavorite(book.id, book.isFavorite) }
                                )
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }

                // Favorites
                if (favorites.isNotEmpty()) {
                    item {
                        SectionHeader("Favorites")
                    }
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            items(favorites, key = { it.id }) { book ->
                                BookCard(
                                    book = book,
                                    onClick = { onBookClick(book.id) },
                                    onFavoriteClick = { viewModel.toggleFavorite(book.id, book.isFavorite) }
                                )
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }

                // Collections shortcut
                item {
                    Card(
                        onClick = onCollectionsClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.FolderOpen, "Collections",
                                tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Collections", style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ChevronRight, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }

                // Reading Lists shortcut
                item {
                    Card(
                        onClick = onReadingListsClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.ListAlt, "Reading Lists",
                                tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Reading Lists", style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ChevronRight, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }

                item {
                    Card(
                        onClick = onFavoriteQuotesClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.FormatQuote, "Favorite Quotes",
                                tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Favorite Quotes", style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ChevronRight, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                // All Books header
                item {
                    SectionHeader("All Books (${allBooks.size})")
                }

                when (layout) {
                    LibraryLayout.GRID -> {
                        val rows = allBooks.chunked(3)
                        items(rows.size) { rowIndex ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rows[rowIndex].forEach { book ->
                                    BookCard(
                                        book = book,
                                        onClick = { onBookClick(book.id) },
                                        onFavoriteClick = { viewModel.toggleFavorite(book.id, book.isFavorite) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                repeat(3 - rows[rowIndex].size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                    LibraryLayout.LIST -> {
                        items(allBooks.size) { index ->
                            BookListItem(
                                book = allBooks[index],
                                onClick = { onBookClick(allBooks[index].id) },
                                onFavoriteClick = { viewModel.toggleFavorite(allBooks[index].id, allBooks[index].isFavorite) },
                                onInfoClick = { onBookInfoClick(allBooks[index].id) },
                                onRenameClick = { showRenameDialog = allBooks[index] },
                                onDeleteClick = { showDeleteDialog = allBooks[index] },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    }
                    LibraryLayout.BOOKSHELF -> {
                        item {
                            BookshelfView(
                                books = allBooks,
                                onBookClick = onBookClick,
                                onFavoriteClick = { viewModel.toggleFavorite(it.id, it.isFavorite) },
                                modifier = Modifier.height(400.dp)
                            )
                        }
                    }
                }
            }
        }

        // Import loading
        if (isImporting) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = MaterialTheme.shapes.large,
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Importing PDF...")
                    }
                }
            }
        }

        // Error snackbar
        importError?.let { error ->
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearImportError() }) {
                        Text("Dismiss")
                    }
                }
            ) {
                Text(error)
            }
        }

        // Delete dialog
        showDeleteDialog?.let { book ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text("Remove Book") },
                text = { Text("Remove \"${book.displayTitle}\" from your library? The PDF file will not be deleted.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteBook(book.id)
                        showDeleteDialog = null
                    }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) { Text("Cancel") }
                }
            )
        }

        showRenameDialog?.let { book ->
            var title by remember(book.id) { mutableStateOf(book.displayTitle) }
            AlertDialog(
                onDismissRequest = { showRenameDialog = null },
                title = { Text("Rename Book") },
                text = {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Library title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.renameBook(book.id, title)
                        showRenameDialog = null
                    }) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameDialog = null }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun BookListContent(
    books: List<BookEntity>,
    layout: LibraryLayout,
    onBookClick: (Long) -> Unit,
    onFavoriteClick: (BookEntity) -> Unit,
    onBookInfoClick: ((Long) -> Unit)? = null,
    onRenameClick: ((BookEntity) -> Unit)? = null,
    onDeleteClick: ((BookEntity) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(books, key = { it.id }) { book ->
            BookListItem(
                book = book,
                onClick = { onBookClick(book.id) },
                onFavoriteClick = { onFavoriteClick(book) },
                onInfoClick = onBookInfoClick?.let { { it(book.id) } },
                onRenameClick = onRenameClick?.let { { it(book) } },
                onDeleteClick = onDeleteClick?.let { { it(book) } }
            )
        }
    }
}

@Composable
fun EmptyLibraryState(onImport: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Outlined.MenuBook,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Your bookshelf is empty",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Import your first PDF and start reading",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onImport,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Default.Add, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Import PDF")
        }
    }
}
