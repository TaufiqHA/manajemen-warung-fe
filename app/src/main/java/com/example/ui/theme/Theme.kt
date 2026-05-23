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
    primary = PrimaryColor,
    onPrimary = SurfaceColor,
    primaryContainer = PrimaryDarkColor,
    onPrimaryContainer = SurfaceColor,
    secondary = InfoColor,
    error = DangerColor,
    background = TextMainColor,
    onBackground = SurfaceColor,
    surface = TextMainColor,
    onSurface = SurfaceColor,
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryColor,
    onPrimary = SurfaceColor,
    primaryContainer = PrimaryDarkColor,
    onPrimaryContainer = SurfaceColor,
    secondary = InfoColor,
    error = DangerColor,
    background = BackgroundColor,
    onBackground = TextMainColor,
    surface = SurfaceColor,
    onSurface = TextMainColor,
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Forced to false to ensure white base color as requested by user
    // Disable dynamic colors by default to enforce brand colors
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
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