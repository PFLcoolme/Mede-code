package com.medecode.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 黑白配色方案 - 深色模式
private val DarkSurfaceColor = Color(0xFF18181B)
private val DarkSecondarySurfaceColor = Color(0xFF27272A)
private val DarkBorderColor = Color(0xFF3F3F46)
private val DarkTextColor = Color(0xFFF5F5F5)
private val DarkSecondaryTextColor = Color(0xFFA1A1AA)
private val DarkAccentColor = Color(0xFFFFFFFF)

// 黑白配色方案 - 浅色模式
private val LightSurfaceColor = Color(0xFFF6F6F7)
private val LightSecondarySurfaceColor = Color(0xFFFFFFFF)
private val LightBorderColor = Color(0xFFE4E4E7)
private val LightTextColor = Color(0xFF18181B)
private val LightSecondaryTextColor = Color(0xFF71717A)
private val LightAccentColor = Color(0xFF000000)

private val DarkColorScheme = darkColorScheme(
    primary = DarkAccentColor,
    onPrimary = Color(0xFF000000),
    secondary = DarkSecondaryTextColor,
    onSecondary = DarkSurfaceColor,
    surface = DarkSurfaceColor,
    onSurface = DarkTextColor,
    surfaceVariant = DarkSecondarySurfaceColor,
    onSurfaceVariant = DarkSecondaryTextColor,
    outline = DarkBorderColor,
    outlineVariant = DarkBorderColor
)

private val LightColorScheme = lightColorScheme(
    primary = LightAccentColor,
    onPrimary = Color(0xFFFFFFFF),
    secondary = LightSecondaryTextColor,
    onSecondary = LightSurfaceColor,
    surface = LightSurfaceColor,
    onSurface = LightTextColor,
    surfaceVariant = LightSecondarySurfaceColor,
    onSurfaceVariant = LightSecondaryTextColor,
    outline = LightBorderColor,
    outlineVariant = LightBorderColor
)

@Composable
fun MedecodeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}