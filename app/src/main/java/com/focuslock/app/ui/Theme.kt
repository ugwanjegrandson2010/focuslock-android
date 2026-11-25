package com.focuslock.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import com.focuslock.app.ui.design.DesignTokens.Palette

private val FocusColors = darkColorScheme(
    primary = Palette.Accent,
    onPrimary = Palette.Void,
    primaryContainer = Palette.AccentDeep,
    onPrimaryContainer = Palette.TextPrimary,
    secondary = Palette.AccentSoft,
    onSecondary = Palette.Void,
    background = Palette.Void,
    onBackground = Palette.TextPrimary,
    surface = Palette.Surface,
    onSurface = Palette.TextPrimary,
    surfaceVariant = Palette.SurfaceRaised,
    onSurfaceVariant = Palette.TextSecondary,
    error = Palette.Danger,
    outline = Palette.Hairline
)

@Composable
fun FocusLockTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = FocusColors, content = content)
}
