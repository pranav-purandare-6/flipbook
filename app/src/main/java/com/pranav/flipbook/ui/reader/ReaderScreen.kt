package com.pranav.flipbook.ui.reader

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pranav.flipbook.pdf.metadata.TocEntry
import com.pranav.flipbook.pdf.search.SearchResult
import com.pranav.flipbook.ui.reader.pagecurl.PageCurlView
import com.pranav.flipbook.viewmodel.ReaderViewModel
import com.pranav.flipbook.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    bookId: Long,
    onBack: () -> Unit,
    onBookmarksClick: () -> Unit,
    onNotesClick: () -> Unit,
    onInfoClick: () -> Unit,
    viewModel: ReaderViewModel = viewModel()
) {
    val book by viewModel.book.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val totalPages by viewModel.totalPages.collectAsState()
    val currentBitmap by viewModel.currentBitmap.collectAsState()
    val nextBitmap by viewModel.nextBitmap.collectAsState()
    val previousBitmap by viewModel.previousBitmap.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val isBookmarked by viewModel.isBookmarked.collectAsState()
    val tocEntries by viewModel.tocEntries.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val transitionStyle by viewModel.transitionStyle.collectAsState()
    val animationDuration by viewModel.animationDuration.collectAsState()
    val appearance by viewModel.appearance.collectAsState()
    val autoHide by viewModel.autoHideControls.collectAsState()

    var showControls by remember { mutableStateOf(true) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showPageJump by remember { mutableStateOf(false) }
    var showSettingsPanel by remember { mutableStateOf(false) }
    var showSaveQuoteDialog by remember { mutableStateOf(false) }
    var showHighlightDialog by remember { mutableStateOf(false) }
    var showSearchSheet by remember { mutableStateOf(false) }
    var showTocSheet by remember { mutableStateOf(false) }
    var isZoomed by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val settingsViewModel: SettingsViewModel = viewModel()

    LaunchedEffect(bookId) { viewModel.openBook(bookId) }

    LaunchedEffect(showControls) {
        val window = (context as? Activity)?.window ?: return@LaunchedEffect
        val controller = WindowInsetsControllerCompat(window, view)
        if (!showControls) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    LaunchedEffect(showControls, autoHide) {
        if (showControls && autoHide) {
            delay(4000)
            showControls = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.endSession()
            val window = (context as? Activity)?.window ?: return@onDispose
            WindowInsetsControllerCompat(window, view).show(WindowInsetsCompat.Type.systemBars())
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> viewModel.onReaderPause()
                Lifecycle.Event.ON_START -> viewModel.onReaderResume()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appearance.backgroundColor)
    ) {
        when {
            isLoading -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Opening book...", color = MaterialTheme.colorScheme.onBackground)
                }
            }
            error != null -> {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(error ?: "Unknown error", color = MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onBack) { Text("Go Back") }
                }
            }
            else -> {
                ZoomablePageView(
                    onZoomChanged = { isZoomed = it },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(appearance.marginDp.dp)
                        .onSizeChanged { viewModel.setViewSize(it.width, it.height) }
                ) {
                    PageCurlView(
                        currentBitmap = currentBitmap,
                        nextBitmap = nextBitmap,
                        previousBitmap = previousBitmap,
                        pageIndex = currentPage,
                        totalPages = totalPages,
                        transitionStyle = transitionStyle,
                        animationDurationMs = animationDuration,
                        onPageForward = { viewModel.nextPage() },
                        onPageBackward = { viewModel.previousPage() },
                        onTurnStart = { viewModel.onPageTurnStart() },
                        onCenterTap = { showControls = !showControls },
                        isZoomed = isZoomed,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Brightness overlay
                if (appearance.brightness < 0.99f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = (1f - appearance.brightness).coerceIn(0f, 0.85f)))
                    )
                }

                // Top bar
                AnimatedVisibility(
                    visible = showControls,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically(),
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    ReaderTopBar(
                        title = book?.displayTitle ?: "",
                        isBookmarked = isBookmarked,
                        onBack = onBack,
                        onBookmark = { viewModel.toggleBookmark() },
                        onMore = { showMoreMenu = true },
                        showMoreMenu = showMoreMenu,
                        onDismissMenu = { showMoreMenu = false },
                        onGoToPage = { showPageJump = true },
                        onBookmarks = onBookmarksClick,
                        onNotes = onNotesClick,
                        onSearch = { showSearchSheet = true },
                        onToc = { showTocSheet = true },
                        onSaveQuote = { showSaveQuoteDialog = true },
                        onAddHighlight = { showHighlightDialog = true },
                        onSettings = { showSettingsPanel = true },
                        onInfo = onInfoClick
                    )
                }

                // Bottom bar
                AnimatedVisibility(
                    visible = showControls,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    ReaderBottomBar(
                        currentPage = currentPage,
                        totalPages = totalPages,
                        onPageChange = { viewModel.goToPage(it) }
                    )
                }
            }
        }

        if (showPageJump) {
            PageJumpDialog(
                currentPage = currentPage + 1,
                totalPages = totalPages,
                onDismiss = { showPageJump = false },
                onJump = { page ->
                    viewModel.goToPage(page - 1)
                    showPageJump = false
                }
            )
        }

        if (showSettingsPanel) {
            ReaderSettingsSheet(
                settingsViewModel = settingsViewModel,
                onDismiss = { showSettingsPanel = false }
            )
        }

        if (showSearchSheet) {
            PdfSearchSheet(
                results = searchResults,
                onSearch = { viewModel.searchInPdf(it) },
                onDismiss = { showSearchSheet = false },
                onOpenResult = { result ->
                    viewModel.goToPage(result.pageIndex)
                    showSearchSheet = false
                }
            )
        }

        if (showTocSheet) {
            TocSheet(
                entries = tocEntries,
                onDismiss = { showTocSheet = false },
                onOpen = { entry ->
                    viewModel.goToPage(entry.pageIndex)
                    showTocSheet = false
                }
            )
        }

        if (showSaveQuoteDialog) {
            SaveQuoteDialog(
                page = currentPage + 1,
                onDismiss = { showSaveQuoteDialog = false },
                onSave = { text ->
                    viewModel.saveFavoriteQuote(text)
                    showSaveQuoteDialog = false
                }
            )
        }

        if (showHighlightDialog) {
            SaveHighlightDialog(
                page = currentPage + 1,
                onDismiss = { showHighlightDialog = false },
                onSave = { text, color ->
                    viewModel.savePageHighlight(text, color)
                    showHighlightDialog = false
                }
            )
        }
    }
}

@Composable
private fun ReaderTopBar(
    title: String,
    isBookmarked: Boolean,
    onBack: () -> Unit,
    onBookmark: () -> Unit,
    onMore: () -> Unit,
    showMoreMenu: Boolean,
    onDismissMenu: () -> Unit,
    onGoToPage: () -> Unit,
    onBookmarks: () -> Unit,
    onNotes: () -> Unit,
    onSearch: () -> Unit,
    onToc: () -> Unit,
    onSaveQuote: () -> Unit,
    onAddHighlight: () -> Unit,
    onSettings: () -> Unit,
    onInfo: () -> Unit
) {
    Surface(color = Color.Black.copy(alpha = 0.72f), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
            }
            Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1, modifier = Modifier.weight(1f))
            IconButton(onClick = onBookmark) {
                Icon(
                    if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    "Bookmark",
                    tint = if (isBookmarked) MaterialTheme.colorScheme.primary else Color.White
                )
            }
            Box {
                IconButton(onClick = onMore) {
                    Icon(Icons.Default.MoreVert, "More", tint = Color.White)
                }
                DropdownMenu(expanded = showMoreMenu, onDismissRequest = onDismissMenu) {
                    DropdownMenuItem(text = { Text("Go to page") }, onClick = { onDismissMenu(); onGoToPage() },
                        leadingIcon = { Icon(Icons.Outlined.Numbers, null) })
                    DropdownMenuItem(text = { Text("Bookmarks") }, onClick = { onDismissMenu(); onBookmarks() },
                        leadingIcon = { Icon(Icons.Outlined.Bookmarks, null) })
                    DropdownMenuItem(text = { Text("Notes & Highlights") }, onClick = { onDismissMenu(); onNotes() },
                        leadingIcon = { Icon(Icons.Outlined.StickyNote2, null) })
                    DropdownMenuItem(text = { Text("Search PDF") }, onClick = { onDismissMenu(); onSearch() },
                        leadingIcon = { Icon(Icons.Outlined.Search, null) })
                    DropdownMenuItem(text = { Text("Table of Contents") }, onClick = { onDismissMenu(); onToc() },
                        leadingIcon = { Icon(Icons.Outlined.ListAlt, null) })
                    DropdownMenuItem(text = { Text("Save Quote") }, onClick = { onDismissMenu(); onSaveQuote() },
                        leadingIcon = { Icon(Icons.Outlined.FormatQuote, null) })
                    DropdownMenuItem(text = { Text("Add Page Highlight") }, onClick = { onDismissMenu(); onAddHighlight() },
                        leadingIcon = { Icon(Icons.Outlined.Highlight, null) })
                    DropdownMenuItem(text = { Text("Reader Settings") }, onClick = { onDismissMenu(); onSettings() },
                        leadingIcon = { Icon(Icons.Outlined.Tune, null) })
                    DropdownMenuItem(text = { Text("PDF Info") }, onClick = { onDismissMenu(); onInfo() },
                        leadingIcon = { Icon(Icons.Outlined.Info, null) })
                }
            }
        }
    }
}

@Composable
private fun SaveHighlightDialog(
    page: Int,
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Unit
) {
    val colors = listOf(
        0xFFFFF176.toInt() to "Yellow",
        0xFFA5D6A7.toInt() to "Green",
        0xFF90CAF9.toInt() to "Blue",
        0xFFF8BBD0.toInt() to "Pink"
    )
    var text by remember { mutableStateOf("") }
    var selectedColor by remember { mutableIntStateOf(colors.first().first) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Page Highlight") },
        text = {
            Column {
                Text("Page $page", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colors.forEach { (color, label) ->
                        FilterChip(
                            selected = selectedColor == color,
                            onClick = { selectedColor = color },
                            label = { Text(label) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Highlighted text or note") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                    maxLines = 8
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onSave(text, selectedColor) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun SaveQuoteDialog(
    page: Int,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save Quote") },
        text = {
            Column {
                Text("Page $page", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Quote") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                    maxLines = 8
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onSave(text) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ReaderBottomBar(
    currentPage: Int,
    totalPages: Int,
    onPageChange: (Int) -> Unit
) {
    Surface(color = Color.Black.copy(alpha = 0.72f), modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (totalPages > 0) {
                Slider(
                    value = currentPage.toFloat(),
                    onValueChange = { onPageChange(it.toInt()) },
                    valueRange = 0f..(totalPages - 1).toFloat().coerceAtLeast(0f),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Page ${currentPage + 1} of $totalPages", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.9f))
                val pct = if (totalPages > 0) ((currentPage + 1) * 100 / totalPages) else 0
                Text("$pct%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PdfSearchSheet(
    results: List<SearchResult>,
    onSearch: (String) -> Unit,
    onDismiss: () -> Unit,
    onOpenResult: (SearchResult) -> Unit
) {
    var query by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
            Text("Search PDF", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    onSearch(it)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Search text") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) }
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                if (query.isBlank()) "Enter text to search this PDF" else "${results.size} result${if (results.size == 1) "" else "s"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (query.isNotBlank() && results.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    Text("No matches found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    items(results, key = { "${it.pageIndex}_${it.contextBefore}_${it.text}" }) { result ->
                        ListItem(
                            headlineContent = { Text("Page ${result.pageIndex + 1}") },
                            supportingContent = {
                                Text(
                                    "${result.contextBefore} ${result.text} ${result.contextAfter}".trim(),
                                    maxLines = 3
                                )
                            },
                            modifier = Modifier.clickable { onOpenResult(result) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TocSheet(
    entries: List<TocEntry>,
    onDismiss: () -> Unit,
    onOpen: (TocEntry) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
            Text("Table of Contents", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(12.dp))
            if (entries.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                    Text("This PDF does not include an outline", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 480.dp)) {
                    entries.forEach { entry ->
                        tocEntryItems(entry, onOpen)
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.tocEntryItems(
    entry: TocEntry,
    onOpen: (TocEntry) -> Unit
) {
    item(key = "${entry.level}_${entry.pageIndex}_${entry.title}") {
        ListItem(
            headlineContent = { Text(entry.title, maxLines = 1) },
            supportingContent = { Text("Page ${entry.pageIndex + 1}") },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpen(entry) }
                .padding(start = (entry.level * 16).dp)
        )
        HorizontalDivider()
    }
    entry.children.forEach { child -> tocEntryItems(child, onOpen) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderSettingsSheet(
    settingsViewModel: SettingsViewModel,
    onDismiss: () -> Unit
) {
    val readerTheme by settingsViewModel.readerTheme.collectAsState()
    val brightness by settingsViewModel.readerBrightness.collectAsState()
    val marginSize by settingsViewModel.marginSize.collectAsState()
    val transitionStyle by settingsViewModel.transitionStyle.collectAsState()
    val animationSpeed by settingsViewModel.animationSpeed.collectAsState()
    val pageSound by settingsViewModel.pageSoundEnabled.collectAsState()
    val pageSoundVolume by settingsViewModel.pageSoundVolume.collectAsState()
    val ambientSound by settingsViewModel.ambientSound.collectAsState()
    val ambientVolume by settingsViewModel.ambientVolume.collectAsState()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).padding(bottom = 32.dp)) {
            Text("Reader Settings", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            Text("Reading Mode", style = MaterialTheme.typography.labelLarge)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("light" to "Light", "dark" to "Dark", "sepia" to "Sepia", "warm" to "Warm", "bw" to "B&W").forEach { (key, label) ->
                    FilterChip(
                        selected = readerTheme == key,
                        onClick = { settingsViewModel.setReaderTheme(key) },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("Brightness", style = MaterialTheme.typography.labelLarge)
            Slider(
                value = brightness,
                onValueChange = { settingsViewModel.setReaderBrightness(it) },
                valueRange = 0.3f..1f
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text("Margins", style = MaterialTheme.typography.labelLarge)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("small" to "Small", "medium" to "Medium", "large" to "Large").forEach { (key, label) ->
                    FilterChip(
                        selected = marginSize == key,
                        onClick = { settingsViewModel.setMarginSize(key) },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("Transition", style = MaterialTheme.typography.labelLarge)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("CURL" to "Curl", "SLIDE" to "Slide", "FADE" to "Fade", "NONE" to "None").forEach { (key, label) ->
                    FilterChip(
                        selected = transitionStyle == key,
                        onClick = { settingsViewModel.setTransitionStyle(key) },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("Animation Speed", style = MaterialTheme.typography.labelLarge)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(900 to "Very Slow", 700 to "Slow", 500 to "Normal", 320 to "Fast", 180 to "Very Fast").forEach { (speed, label) ->
                    FilterChip(
                        selected = animationSpeed == speed,
                        onClick = { settingsViewModel.setAnimationSpeed(speed) },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Page Turn Sound", style = MaterialTheme.typography.labelLarge)
            Switch(checked = pageSound, onCheckedChange = { settingsViewModel.setPageSound(it) })
            Slider(
                value = pageSoundVolume,
                onValueChange = { settingsViewModel.setPageSoundVolume(it) },
                valueRange = 0f..1f
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text("Ambient Sound", style = MaterialTheme.typography.labelLarge)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("none" to "None", "rain" to "Rain", "fireplace" to "Fire", "cafe" to "Cafe", "forest" to "Forest", "whitenoise" to "Noise").forEach { (key, label) ->
                    FilterChip(
                        selected = ambientSound == key,
                        onClick = { settingsViewModel.setAmbientSound(key) },
                        label = { Text(label) }
                    )
                }
            }
            Slider(
                value = ambientVolume,
                onValueChange = { settingsViewModel.setAmbientVolume(it) },
                valueRange = 0f..1f
            )
        }
    }
}

@Composable
fun PageJumpDialog(
    currentPage: Int,
    totalPages: Int,
    onDismiss: () -> Unit,
    onJump: (Int) -> Unit
) {
    var pageText by remember { mutableStateOf(currentPage.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Go to Page") },
        text = {
            Column {
                Text("Enter page number (1-$totalPages)")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = pageText,
                    onValueChange = { pageText = it.filter { c -> c.isDigit() } },
                    singleLine = true,
                    label = { Text("Page") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                pageText.toIntOrNull()?.let { if (it in 1..totalPages) onJump(it) }
            }) { Text("Go") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
