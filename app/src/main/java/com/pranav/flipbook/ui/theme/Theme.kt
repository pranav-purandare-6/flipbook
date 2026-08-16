package com.pranav.flipbook.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = BookDarkPrimary,
    onPrimary = BookDarkOnPrimary,
    secondary = BookDarkSecondary,
    tertiary = BookDarkTertiary,
    background = BookDarkBackground,
    surface = BookDarkSurface,
    surfaceVariant = BookDarkSurfaceVariant,
    onBackground = BookDarkOnBackground,
    onSurface = BookDarkOnSurface,
    onSurfaceVariant = BookDarkOnSurfaceVariant,
    primaryContainer = BookDarkSurfaceVariant,
    onPrimaryContainer = BookDarkTertiary,
    secondaryContainer = BookDarkSurfaceVariant,
    onSecondaryContainer = BookDarkOnSurface,
    outline = WarmGray,
    outlineVariant = BookDarkSurfaceVariant
)

private val LightColorScheme = lightColorScheme(
    primary = BookLightPrimary,
    onPrimary = BookLightOnPrimary,
    secondary = BookLightSecondary,
    tertiary = BookLightTertiary,
    background = BookLightBackground,
    surface = BookLightSurface,
    surfaceVariant = BookLightSurfaceVariant,
    onBackground = BookLightOnBackground,
    onSurface = BookLightOnSurface,
    onSurfaceVariant = BookLightOnSurfaceVariant,
    primaryContainer = BookLightSurfaceVariant,
    onPrimaryContainer = BookLightPrimary,
    secondaryContainer = BookLightSurfaceVariant,
    onSecondaryContainer = BookLightOnSurface,
    outline = WarmGray,
    outlineVariant = BookLightSurfaceVariant
)

@Composable
fun FlipBookTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}