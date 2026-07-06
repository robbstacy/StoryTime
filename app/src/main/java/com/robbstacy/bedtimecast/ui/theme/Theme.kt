package com.robbstacy.bedtimecast.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFFE07856),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFFC4536A),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFFFF8F0),
    onBackground = Color(0xFF2D2A32),
    surface = Color(0xFFFFF8F0),
    onSurface = Color(0xFF2D2A32),
    surfaceVariant = Color(0xFFF6EDE2),
    onSurfaceVariant = Color(0xFF6E6659),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF2955F),
    onPrimary = Color(0xFF2D2A32),
    secondary = Color(0xFFE58BA0),
    onSecondary = Color(0xFF2D2A32),
    background = Color(0xFF1A1B2E),
    onBackground = Color(0xFFF2EDE6),
    surface = Color(0xFF1A1B2E),
    onSurface = Color(0xFFF2EDE6),
    surfaceVariant = Color(0xFF25263C),
    onSurfaceVariant = Color(0xFFA8A5B8),
)

/** Extra palette entries beyond the Material scheme. */
object AppColors {
    val recorded: Color
        @Composable get() = if (isSystemInDarkTheme()) Color(0xFFE58BA0) else Color(0xFFC4536A)
    val record: Color
        @Composable get() = if (isSystemInDarkTheme()) Color(0xFFE05A5A) else Color(0xFFD64545)
    val gold: Color
        @Composable get() = if (isSystemInDarkTheme()) Color(0xFFF2B950) else Color(0xFFDFA53E)
    val selected: Color
        @Composable get() = if (isSystemInDarkTheme()) Color(0xFF31324C) else Color(0xFFEFE2D2)
}

/** Stable display colors for characters within a story. */
val CHARACTER_COLORS = listOf(
    Color(0xFFC4536A),
    Color(0xFF7C6FB0),
    Color(0xFF3E8E7E),
    Color(0xFFB8860B),
    Color(0xFF5B7DB1),
    Color(0xFFA0623F),
)

@Composable
fun BedtimeCastTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
