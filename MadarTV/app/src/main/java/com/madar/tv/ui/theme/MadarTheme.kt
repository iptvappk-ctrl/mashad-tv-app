package com.madar.tv.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

object MashadColors {
    val Background = Color(0xFF0C0D12)          // Deep Obsidian Cinema Dark
    val Surface = Color(0xFF161821)             // Dark Slate Surface
    val SurfaceCard = Color(0xFF1B1E29)         // Elevated card surface
    val SurfaceHighlight = Color(0xFF252937)    // Elevated chip/button
    val Gold = Color(0xFFD4AF37)                // Royal Luxury Gold (from Logo)
    val GoldLight = Color(0xFFF3C766)           // Radiant Champagne Gold
    val GoldDark = Color(0xFF9E7B1D)            // Deep Amber Gold
    val GoldBorder = Color(0x40D4AF37)          // Subtle Gold Glow Border
    val TextWhite = Color(0xFFFFFFFF)           // Crisp White
    val TextMuted = Color(0xFF9EA3B5)           // Muted Text
    val Border = Color(0xFF242735)              // Card border
    val LiveGreen = Color(0xFF00E676)           // Match live green
    val Red = Color(0xFFE53935)                 // Tag / Alert red

    // Aliases for legacy code
    val SurfaceSoft = SurfaceCard
    val Line = Border
    val Text = TextWhite
    val Muted = TextMuted
    val Accent = Gold
    val AccentSoft = GoldBorder
    val Mint = LiveGreen
    val Violet = GoldLight
    val Danger = Red
}

typealias MadarColors = MashadColors

private val MashadPalette = darkColorScheme(
    primary = MashadColors.Gold,
    onPrimary = Color.Black,
    secondary = MashadColors.GoldLight,
    onSecondary = Color.Black,
    background = MashadColors.Background,
    onBackground = MashadColors.TextWhite,
    surface = MashadColors.Surface,
    onSurface = MashadColors.TextWhite,
    surfaceVariant = MashadColors.SurfaceCard,
    onSurfaceVariant = MashadColors.TextMuted,
    outline = MashadColors.Border,
    error = MashadColors.Red,
)

@Composable
fun MadarTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = MashadPalette, content = content)
    }
}
