package com.oxygen.jadwalin.ui.compose

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class JadwalinColorSystem(
    val primary: Color,
    val primaryDark: Color,
    val background: Color,
    val surface: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textHint: Color,
    val divider: Color,
    val success: Color,
    val error: Color,
    val cardBackground: Color,
    val pillBackground: Color,
    val heroGradient: List<Color>,
    // Category Colors
    val catKuliah: Color,
    val catKerja: Color,
    val catPribadi: Color
)

val LightColors = JadwalinColorSystem(
    primary = Color(0xFF2563EB),
    primaryDark = Color(0xFF1D4ED8),
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF475569),
    textHint = Color(0xFF94A3B8),
    divider = Color(0xFFE2E8F0),
    success = Color(0xFF10B981),
    error = Color(0xFFEF4444),
    cardBackground = Color(0xFFFFFFFF),
    pillBackground = Color(0xFFF1F5F9),
    heroGradient = listOf(Color(0xFF2563EB), Color(0xFF1D4ED8)),
    catKuliah = Color(0xFF4F46E5),
    catKerja = Color(0xFFD97706),
    catPribadi = Color(0xFF059669)
)

val DarkColors = JadwalinColorSystem(
    primary = Color(0xFF60A5FA),
    primaryDark = Color(0xFF3B82F6),
    background = Color(0xFF020617),
    surface = Color(0xFF0F172A),
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xFF94A3B8),
    textHint = Color(0xFF64748B),
    divider = Color(0xFF1E293B),
    success = Color(0xFF34D399),
    error = Color(0xFFF87171),
    cardBackground = Color(0xFF0F172A),
    pillBackground = Color(0xFF1E293B),
    heroGradient = listOf(Color(0xFF1E3A8A), Color(0xFF172554)),
    catKuliah = Color(0xFF818CF8),
    catKerja = Color(0xFFFBBF24),
    catPribadi = Color(0xFF34D399)
)

val LocalJadwalinColors = staticCompositionLocalOf { LightColors }

object JadwalinTheme {
    val colors: JadwalinColorSystem
        @Composable
        @ReadOnlyComposable
        get() = LocalJadwalinColors.current
    
    val gradient: Brush
        @Composable
        @ReadOnlyComposable
        get() = Brush.linearGradient(colors.heroGradient)
}

object JadwalinShapes {
    val Card = RoundedCornerShape(24.dp)
    val Button = RoundedCornerShape(16.dp)
    val Chip = RoundedCornerShape(14.dp)
    val Input = RoundedCornerShape(16.dp)
}
