package com.mireru.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary            = MireruAccent,
    onPrimary          = MireruBg,
    primaryContainer   = Color(0xFF003D30),
    onPrimaryContainer = MireruAccent,

    secondary          = MireruAccent3,
    onSecondary        = MireruBg,
    secondaryContainer = Color(0xFF1A1E35),
    onSecondaryContainer = MireruAccent3,

    tertiary           = MireruGold,
    onTertiary         = MireruBg,

    background         = MireruBg,
    onBackground       = MireruText,

    surface            = MireruSurface,
    onSurface          = MireruText,
    surfaceVariant     = MireruSurface2,
    onSurfaceVariant   = MireruText2,

    outline            = MireruBorder,
    outlineVariant     = Color(0xFF1E2230),

    error              = MireruDanger,
    onError            = Color.White,
    errorContainer     = Color(0xFF3D0010),
    onErrorContainer   = MireruDanger,
)

@Composable
fun MireruTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = MireruTypography,
        content     = content
    )
}
