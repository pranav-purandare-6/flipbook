package com.pranav.flipbook.ui.info

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pranav.flipbook.utils.formatDate
import com.pranav.flipbook.utils.formatDuration
import com.pranav.flipbook.utils.formatFileSize
import com.pranav.flipbook.utils.toProgressPercent
import com.pranav.flipbook.viewmodel.LibraryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfInfoScreen(
    bookId: Long,
    onBack: () -> Unit,
    viewModel: LibraryViewModel = viewModel()
) {
    val allBooks by viewModel.allBooks.collectAsState()
    val book = allBooks.find { it.id == bookId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PDF Information") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        if (book == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Book not found")
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item {
                    Text("Document", style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item { InfoRow("Title", book.displayTitle) }
                item { InfoRow("File Name", book.fileName) }
                book.author?.let { item { InfoRow("Author", it) } }
                book.subject?.let { item { InfoRow("Subject", it) } }
                book.creator?.let { item { InfoRow("Creator", it) } }
                book.producer?.let { item { InfoRow("Producer", it) } }
                item { InfoRow("Page Count", "${book.pageCount}") }
                item { InfoRow("File Size", book.fileSize.formatFileSize()) }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Reading", style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item { InfoRow("Current Page", "${book.currentPage + 1}") }
                item { InfoRow("Progress", "${book.readingProgress.toProgressPercent()}%") }
                item { InfoRow("Date Added", book.dateAdded.formatDate()) }
                book.lastOpened?.let { item { InfoRow("Last Opened", it.formatDate()) } }
                item { InfoRow("Total Reading Time", book.totalReadingTime.formatDuration()) }
                item { InfoRow("Total Pages Read", "${book.totalPagesRead}") }
                item { InfoRow("Completed", if (book.isCompleted) "Yes" else "No") }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.6f)
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
}
