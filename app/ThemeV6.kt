package com.example.tradejournal

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

class Pal(
    val bg: Color, val card: Color, val card2: Color, val text: Color,
    val muted: Color, val up: Color, val dn: Color
)

val DarkPal = Pal(
    Color(0xFF0E1330), Color(0xFF171D45), Color(0xFF202862), Color(0xFFF3F1EA),
    Color(0xFF98A0C8), Color(0xFF3FD0C9), Color(0xFFFF6B6B)
)
val LightPal = Pal(
    Color(0xFFF3F1EA), Color(0xFFFFFFFF), Color(0xFFEAE7DC), Color(0xFF141A3D),
    Color(0xFF5E668F), Color(0xFF0E9C96), Color(0xFFD9443F)
)

val ACCENTS = listOf(Color(0xFFF2A93B), Color(0xFF3FD0C9), Color(0xFFFF7AA8), Color(0xFFA78BFA))

val LocalPal = staticCompositionLocalOf { DarkPal }
val LocalAccent = staticCompositionLocalOf { ACCENTS[0] }
val LocalMascot = staticCompositionLocalOf { 0 }

val ColOnSaffron = Color(0xFF1A1200)
val ColBg: Color @Composable get() = LocalPal.current.bg
val ColCard: Color @Composable get() = LocalPal.current.card
val ColCard2: Color @Composable get() = LocalPal.current.card2
val ColText: Color @Composable get() = LocalPal.current.text
val ColMuted: Color @Composable get() = LocalPal.current.muted
val ColUp: Color @Composable get() = LocalPal.current.up
val ColDn: Color @Composable get() = LocalPal.current.dn
val ColSaffron: Color @Composable get() = LocalAccent.current

/** mode: 0 خودکار، 1 شب، 2 روز */
@Composable
fun AppTheme(mode: Int, accentIdx: Int, mascot: Int, content: @Composable () -> Unit) {
    val dark = when (mode) { 1 -> true; 2 -> false; else -> isSystemInDarkTheme() }
    val pal = if (dark) DarkPal else LightPal
    val accent = ACCENTS.getOrElse(accentIdx) { ACCENTS[0] }
    val scheme = if (dark) darkColorScheme(
        primary = accent, onPrimary = ColOnSaffron,
        background = pal.bg, onBackground = pal.text,
        surface = pal.card, onSurface = pal.text,
        surfaceVariant = pal.card2, onSurfaceVariant = pal.muted,
        secondaryContainer = accent.copy(alpha = 0.2f), onSecondaryContainer = accent,
        outline = pal.muted
    ) else lightColorScheme(
        primary = accent, onPrimary = ColOnSaffron,
        background = pal.bg, onBackground = pal.text,
        surface = pal.card, onSurface = pal.text,
        surfaceVariant = pal.card2, onSurfaceVariant = pal.muted,
        secondaryContainer = accent.copy(alpha = 0.2f), onSecondaryContainer = pal.text,
        outline = pal.muted
    )
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(
            LocalPal provides pal,
            LocalAccent provides accent,
            LocalMascot provides mascot,
            LocalLayoutDirection provides (if (Lang.en) LayoutDirection.Ltr else LayoutDirection.Rtl),
            content = content
        )
    }
}
