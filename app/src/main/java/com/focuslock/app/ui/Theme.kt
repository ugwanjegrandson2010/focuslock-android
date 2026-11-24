package com.focuslock.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9DB4FF),
    onPrimary = Color(0xFF0B1020),
    primaryContainer = Color(0xFF2B3A63),
    onPrimaryContainer = Color(0xFFDCE5FF),
    secondary = Color(0xFFB8C6FF),
    background = Color(0xFF0B1020),
    onBackground = Color(0xFFE6EBFF),
    surface = Color(0xFF131B31),
    onSurface = Color(0xFFE6EBFF),
    surfaceVariant = Color(0xFF1D2742),
    onSurfaceVariant = Color(0xFFB6C2E6),
    error = Color(0xFFFF8A80)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF3F5BB7),
    onPrimary = Color.White,
    background = Color(0xFFF7F9FF),
    onBackground = Color(0xFF131B31),
    surface = Color.White,
    onSurface = Color(0xFF131B31)
)

@Composable
fun FocusLockTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
