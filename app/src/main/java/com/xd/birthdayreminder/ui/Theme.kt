package com.xd.birthdayreminder.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 一套暖色调：珊瑚橘 + 奶油底，跟"生日"这个主题搭
private val LightColors = lightColorScheme(
    primary = Color(0xFFE8623C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE0D6),
    onPrimaryContainer = Color(0xFF5C1B08),
    secondary = Color(0xFF7A6A5F),
    onSecondary = Color.White,
    background = Color(0xFFFFF8F0),
    onBackground = Color(0xFF2E2521),
    surface = Color(0xFFFFFBF6),
    onSurface = Color(0xFF2E2521),
    surfaceVariant = Color(0xFFF5E9DF),
    onSurfaceVariant = Color(0xFF6B5A4E),
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB59B),
    onPrimary = Color(0xFF5C1B08),
    primaryContainer = Color(0xFF8A3A1E),
    onPrimaryContainer = Color(0xFFFFDBCE),
    secondary = Color(0xFFD9C2B2),
    onSecondary = Color(0xFF3D2E25),
    background = Color(0xFF1C1512),
    onBackground = Color(0xFFEFE0D7),
    surface = Color(0xFF241B17),
    onSurface = Color(0xFFEFE0D7),
    surfaceVariant = Color(0xFF3A2E28),
    onSurfaceVariant = Color(0xFFD7C3B8),
    error = Color(0xFFF2B8B5),
)

@Composable
fun BirthdayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
