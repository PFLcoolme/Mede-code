package com.medecode.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = android.ui.graphics.Color(0xFF82ABD9),
    secondary = android.ui.graphics.Color(0xFF42C3A0),
    tertiary = android.ui.graphics.Color(0xFFB275D5)
)

private val LightColorScheme = lightColorScheme(
    primary = android.ui.graphics.Color(0xFF3B82F6),
    secondary = android.ui.graphics.Color(0xFF10B981),
    tertiary = android.ui.graphics.Color(0xFF8B5CF6)
)

@Composable
fun MedecodeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}