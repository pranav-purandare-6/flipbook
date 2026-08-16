package com.pranav.flipbook.ui.reader

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pranav.flipbook.ui.reader.pagecurl.PageCurlView
import com.pranav.flipbook.ui.reader.pagecurl.PageTransitionStyle
import com.pranav.flipbook.utils.toProgressPercent
import com.pranav.flipbook.viewmodel.ReaderViewModel
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
    val transitionStyle by viewModel.transitionStyle.collectAsState()
    val animationDuration by viewModel.animationDuration.collectAsState()

    var showControls by remember { mutableStateOf(true) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showPageJump by remember { mutableStateOf(false) }
    var showSettingsPanel by remember { mutableStateOf(false) }
    var isZoomed by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val view = LocalView.current

    // Open book once
    LaunchedEffect(bookId) {
        viewModel.openBook(bookId)
    }

    // Immersive mode
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

    // Auto-hide controls
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(4000)
            showControls = false
        }
    }

    // End session on back
    DisposableEffect(Unit) {
        onDispose {
            viewModel.endSession()
            // Restore system bars
            val window = (context as? Activity)?.window ?: return@onDispose
            val controller = WindowInsetsControllerCompat(window, view)
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            isLoading -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Opening book...", color = Color.White.copy(alpha = 0.7f))
                }
            }
            error != null -> {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Error, null, tint = Color(0xFFEF5350), modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(error ?: "Unknown error", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onBack) { Text("Go Back") }
                }
            }
            else -> {
                // Page content
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
                    onCenterTap = { showControls = !showControls },
                    isZoomed = isZoomed,
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { size ->
                            viewModel.setViewSize(size.width, size.height)
                        }
                )

                // Top controls
                AnimatedVisibility(
                    visible = showControls,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically(),
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                            }
                            Text(
                                text = book?.displayTitle ?: "",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.toggleBookmark() }) {
                                Icon(
                                    if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                    "Bookmark",
                                    tint = if (isBookmarked) MaterialTheme.colorScheme.primary else Color.White
                                )
                            }
                            Box {
                                IconButton(onClick = { showMoreMenu = true }) {
                                    Icon(Icons.Default.MoreVert, "More", tint = Color.White)
                                }
                                DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Go to page") },
                                        onClick = { showMoreMenu = false; showPageJump = true },
                                        leadingIcon = { Icon(Icons.Outlined.Numbers, null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Bookmarks") },
                                        onClick = { showMoreMenu = false; onBookmarksClick() },
                                        leadingIcon = { Icon(Icons.Outlined.Bookmarks, null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Notes & Highlights") },
                                        onClick = { showMoreMenu = false; onNotesClick() },
                                        leadingIcon = { Icon(Icons.Outlined.StickyNote2, null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Reader Settings") },
                                        onClick = { showMoreMenu = false; showSettingsPanel = true },
                                        leadingIcon = { Icon(Icons.Outlined.Tune, null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("PDF Info") },
                                        onClick = { showMoreMenu = false; onInfoClick() },
                                        leadingIcon = { Icon(Icons.Outlined.Info, null) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom controls
                AnimatedVisibility(
                    visible = showControls,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            // Page slider
                            if (totalPages > 0) {
                                Slider(
                                    value = currentPage.toFloat(),
                                    onValueChange = { viewModel.goToPage(it.toInt()) },
                                    valueRange = 0f..(totalPages - 1).toFloat().coerceAtLeast(0f),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Page ${currentPage + 1} of $totalPages",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                val progress = if (totalPages > 0) {
                                    ((currentPage + 1).toFloat() / totalPages * 100).toInt()
                                } else 0
                                Text(
                                    text = "$progress%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Page jump dialog
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
                Text("Enter page number (1–$totalPages)")
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
                val page = pageText.toIntOrNull()
                if (page != null && page in 1..totalPages) {
                    onJump(page)
                }
            }) { Text("Go") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
