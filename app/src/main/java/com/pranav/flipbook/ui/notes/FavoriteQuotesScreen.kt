package com.pranav.flipbook.ui.notes

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pranav.flipbook.data.entity.FavoriteQuoteEntity
import com.pranav.flipbook.utils.formatDate
import com.pranav.flipbook.viewmodel.FavoriteQuoteItem
import com.pranav.flipbook.viewmodel.FavoriteQuotesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteQuotesScreen(
    onBack: () -> Unit,
    onNavigateToPage: (Long, Int) -> Unit,
    viewModel: FavoriteQuotesViewModel = viewModel()
) {
    val quotes by viewModel.quotes.collectAsState()
    val query by viewModel.searchQuery.collectAsState()
    var editingQuote by remember { mutableStateOf<FavoriteQuoteEntity?>(null) }
    var deletingQuote by remember { mutableStateOf<FavoriteQuoteEntity?>(null) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Favorite Quotes") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::setSearchQuery,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                singleLine = true,
                label = { Text("Search quotes") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, "Clear search")
                        }
                    }
                }
            )

            if (quotes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.FormatQuote,
                            null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            if (query.isBlank()) "No saved quotes yet" else "No matching quotes",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(quotes, key = { it.quote.id }) { item ->
                        QuoteCard(
                            item = item,
                            onOpen = { onNavigateToPage(item.quote.bookId, item.quote.page) },
                            onEdit = { editingQuote = item.quote },
                            onDelete = { deletingQuote = item.quote },
                            onShare = {
                                val shareText = buildString {
                                    appendLine(item.quote.text)
                                    append("- ")
                                    append(item.book?.displayTitle ?: "Book")
                                    append(", page ")
                                    append(item.quote.page + 1)
                                }
                                context.startActivity(
                                    Intent.createChooser(
                                        Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                        },
                                        "Share quote"
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    editingQuote?.let { quote ->
        var text by remember(quote.id) { mutableStateOf(quote.text) }
        AlertDialog(
            onDismissRequest = { editingQuote = null },
            title = { Text("Edit Quote") },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                    label = { Text("Quote") },
                    maxLines = 8
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateQuote(quote, text)
                    editingQuote = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editingQuote = null }) { Text("Cancel") } }
        )
    }

    deletingQuote?.let { quote ->
        AlertDialog(
            onDismissRequest = { deletingQuote = null },
            title = { Text("Delete Quote") },
            text = { Text("Delete this saved quote?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteQuote(quote)
                    deletingQuote = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deletingQuote = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun QuoteCard(
    item: FavoriteQuoteItem,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "\"${item.quote.text}\"",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${item.book?.displayTitle ?: "Book"} - Page ${item.quote.page + 1} - ${item.quote.createdDate.formatDate()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onOpen, contentPadding = PaddingValues(horizontal = 4.dp)) { Text("Open") }
                TextButton(onClick = onEdit, contentPadding = PaddingValues(horizontal = 4.dp)) { Text("Edit") }
                TextButton(onClick = onShare, contentPadding = PaddingValues(horizontal = 4.dp)) { Text("Share") }
                TextButton(onClick = onDelete, contentPadding = PaddingValues(horizontal = 4.dp)) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
