package com.raiz.toc.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFF2E6B4F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDEBD7),
    onPrimaryContainer = Color(0xFF0B2E1E),
    secondary = Color(0xFF8A6A3A),
    secondaryContainer = Color(0xFFF3E4C6),
    onSecondaryContainer = Color(0xFF2E2108),
    tertiary = Color(0xFF3C6E8F),
    tertiaryContainer = Color(0xFFD3E6F4),
    background = Color(0xFFF4F7F2),
    surface = Color(0xFFF4F7F2),
    surfaceContainer = Color(0xFFE9EFE7),
    surfaceContainerHigh = Color(0xFFE2EAE0),
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFF9DEDC),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF8FD3A8),
    onPrimary = Color(0xFF00391F),
    primaryContainer = Color(0xFF1F4D3A),
    onPrimaryContainer = Color(0xFFCDEBD7),
    secondary = Color(0xFFE2C48F),
    secondaryContainer = Color(0xFF4A3B1E),
    onSecondaryContainer = Color(0xFFF3E4C6),
    tertiary = Color(0xFF9CCBEA),
    tertiaryContainer = Color(0xFF1F4257),
    background = Color(0xFF0F1714),
    surface = Color(0xFF0F1714),
    surfaceContainer = Color(0xFF18221E),
    surfaceContainerHigh = Color(0xFF212C27),
    error = Color(0xFFF2B8B5),
    errorContainer = Color(0xFF601410),
)

@Composable
fun RaizTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
