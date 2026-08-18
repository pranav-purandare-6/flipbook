package com.pranav.flipbook.ui.reader

import androidx.compose.ui.graphics.Color

enum class ReaderMode(val key: String) {
    LIGHT("light"),
    DARK("dark"),
    SEPIA("sepia"),
    WARM("warm"),
    BLACK_WHITE("bw");

    companion object {
        fun fromKey(key: String): ReaderMode =
            entries.find { it.key == key } ?: LIGHT
    }
}

data class ReaderAppearance(
    val mode: ReaderMode = ReaderMode.LIGHT,
    val brightness: Float = 1f,
    val marginDp: Int = 12
) {
    val backgroundColor: Color
        get() = when (mode) {
            ReaderMode.LIGHT -> Color(0xFFFFFAF2)
            ReaderMode.DARK -> Color(0xFF1A1410)
            ReaderMode.SEPIA -> Color(0xFFF4E8D1)
            ReaderMode.WARM -> Color(0xFFFFF3E0)
            ReaderMode.BLACK_WHITE -> Color(0xFFF8F8F8)
        }

    val cacheKey: String = mode.key
}
