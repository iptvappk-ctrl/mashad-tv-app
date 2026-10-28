package com.madar.tv.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import com.madar.tv.R

object MashadColors {
    // Warm cinematic light palette: sandstone canvas, coral action color, plum titles.
    val Background = Color(0xFFF0E5D7)
    val Surface = Color(0xFFF6EEE3)
    val SurfaceCard = Color(0xFFFBF5EC)
    val SurfaceHighlight = Color(0xFFE7D6C4)
    val Gold = Color(0xFFBF4937)
    val GoldLight = Color(0xFF713D59)
    val GoldDark = Color(0xFF7D3039)
    val GoldBorder = Color(0xFFDCC4AD)
    val TextWhite = Color(0xFF2B2430)
    val TextMuted = Color(0xFF746A70)
    val Border = Color(0xFFD8C9BA)
    val LiveGreen = Color(0xFF16816D)
    val Red = Color(0xFFBF3B54)

    // Aliases used by existing cinema and live-TV composables.
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

private val MashadPalette = lightColorScheme(
    primary = MashadColors.Gold,
    onPrimary = Color.White,
    secondary = MashadColors.GoldLight,
    onSecondary = Color.White,
    tertiary = MashadColors.LiveGreen,
    onTertiary = Color.White,
    background = MashadColors.Background,
    onBackground = MashadColors.TextWhite,
    surface = MashadColors.Surface,
    onSurface = MashadColors.TextWhite,
    surfaceVariant = MashadColors.SurfaceHighlight,
    onSurfaceVariant = MashadColors.TextMuted,
    outline = MashadColors.Border,
    error = MashadColors.Red,
)

val CairoFontFamily = FontFamily(
    Font(R.font.cairo, FontWeight.Normal),
    Font(R.font.cairo, FontWeight.Medium),
    Font(R.font.cairo, FontWeight.Bold),
    Font(R.font.cairo, FontWeight.ExtraBold),
)

val MashadTypography = Typography(
    displayLarge = TextStyle(fontFamily = CairoFontFamily),
    displayMedium = TextStyle(fontFamily = CairoFontFamily),
    displaySmall = TextStyle(fontFamily = CairoFontFamily),
    headlineLarge = TextStyle(fontFamily = CairoFontFamily),
    headlineMedium = TextStyle(fontFamily = CairoFontFamily),
    headlineSmall = TextStyle(fontFamily = CairoFontFamily),
    titleLarge = TextStyle(fontFamily = CairoFontFamily),
    titleMedium = TextStyle(fontFamily = CairoFontFamily),
    titleSmall = TextStyle(fontFamily = CairoFontFamily),
    bodyLarge = TextStyle(fontFamily = CairoFontFamily),
    bodyMedium = TextStyle(fontFamily = CairoFontFamily),
    bodySmall = TextStyle(fontFamily = CairoFontFamily),
    labelLarge = TextStyle(fontFamily = CairoFontFamily),
    labelMedium = TextStyle(fontFamily = CairoFontFamily),
    labelSmall = TextStyle(fontFamily = CairoFontFamily),
)

@Composable
fun MadarTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = MashadPalette, typography = MashadTypography, content = content)
    }
}
