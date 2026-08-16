package com.pranav.flipbook.ui.library

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pranav.flipbook.data.entity.BookEntity

@Composable
fun BookshelfView(
    books: List<BookEntity>,
    onBookClick: (Long) -> Unit,
    onFavoriteClick: (BookEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1E1713))
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(books, key = { it.id }) { book ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onBookClick(book.id) }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .shadow(12.dp, RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 8.dp, bottomEnd = 8.dp))
                            .clip(RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 8.dp, bottomEnd = 8.dp))
                            .background(Color(0xFF2D241E))
                    ) {
                        val coverBitmap = book.coverPath?.let { path ->
                            try { BitmapFactory.decodeFile(path)?.asImageBitmap() } catch (_: Exception) { null }
                        }

                        if (coverBitmap != null) {
                            Image(
                                bitmap = coverBitmap,
                                contentDescription = book.displayTitle,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0xFFC9956B),
                                                Color(0xFF8B5E3C)
                                            )
                                        )
                                    )
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = book.displayTitle,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Spine shadow overlay for book depth
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(8.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent)
                                    )
                                )
                        )
                    }

                    // Wooden Shelf Line below books
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .shadow(4.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFF8B5E3C), Color(0xFF5C3A21))
                                )
                            )
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = book.displayTitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFE8DDD4),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
