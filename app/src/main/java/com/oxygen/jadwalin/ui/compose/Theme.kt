package com.oxygen.jadwalin.ui.compose

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = DarkColors.primary,
    secondary = DarkColors.primaryDark,
    background = DarkColors.background,
    surface = DarkColors.surface,
    onPrimary = Color.White,
    onBackground = DarkColors.textPrimary,
    onSurface = DarkColors.textPrimary,
    surfaceVariant = DarkColors.cardBackground,
    outline = DarkColors.divider
)

private val LightColorScheme = lightColorScheme(
    primary = LightColors.primary,
    secondary = LightColors.primaryDark,
    background = LightColors.background,
    surface = LightColors.surface,
    onPrimary = Color.White,
    onBackground = LightColors.textPrimary,
    onSurface = LightColors.textPrimary,
    surfaceVariant = LightColors.cardBackground,
    outline = LightColors.divider
)

@Composable
fun JadwalinTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorSystem = if (darkTheme) DarkColors else LightColors
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorSystem.background.toArgb()
            window.navigationBarColor = colorSystem.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalJadwalinColors provides colorSystem) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
