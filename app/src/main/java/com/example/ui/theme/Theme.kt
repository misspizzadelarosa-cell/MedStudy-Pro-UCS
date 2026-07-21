package com.example.ui.theme

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
    primary = VibrantHighlight,
    secondary = VibrantPrimaryContainer,
    tertiary = GoldStreak,
    background = VibrantPrimary,
    surface = VibrantPrimary,
    onPrimary = VibrantOnPrimaryContainer,
    onSecondary = VibrantOnPrimaryContainer,
    onBackground = VibrantBackground,
    onSurface = VibrantBackground
)

private val LightColorScheme = lightColorScheme(
    primary = VibrantPrimary,
    secondary = VibrantPrimaryContainer,
    tertiary = GoldStreak,
    background = VibrantBackground,
    surface = VibrantSurface,
    onPrimary = VibrantSurface,
    onSecondary = VibrantOnPrimaryContainer,
    onBackground = SlateTextPrimary,
    onSurface = SlateTextPrimary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

