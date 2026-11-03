package com.example.cardwallet

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Accent = Color(0xFF7B2CBF)

val cardColors = listOf(
    listOf(Color(0xFF9B3FD1), Color(0xFF7B2CBF)),
    listOf(Color(0xFFFFD25E), Color(0xFFF5B82E)),
    listOf(Color(0xFF2D6CDF), Color(0xFF1B4DB3)),
    listOf(Color(0xFF1FA67A), Color(0xFF137A5A)),
    listOf(Color(0xFFE5484D), Color(0xFFB4323A))
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val scheme = if (dark) darkColorScheme(
        background = Color.Black, surface = Color(0xFF151515),
        onBackground = Color.White, onSurface = Color.White,
        primary = Color.White, onPrimary = Color.Black
    ) else lightColorScheme(
        background = Color(0xFFF1F1F1), surface = Color.White,
        onBackground = Color.Black, onSurface = Color.Black,
        primary = Color.Black, onPrimary = Color.White
    )
    MaterialTheme(colorScheme = scheme, content = content)
}
