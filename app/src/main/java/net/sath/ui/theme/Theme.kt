package net.sath.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = Color.White,
    primaryContainer = AccentBlueDark,
    onPrimaryContainer = TextWhite,
    secondary = NeonPurple,
    onSecondary = Color.White,
    secondaryContainer = NeonPurpleDark,
    onSecondaryContainer = TextWhite,
    tertiary = TeleCyan,
    onTertiary = Color.Black,
    background = BgTop,
    onBackground = TextWhite,
    surface = PanelDark,
    onSurface = TextWhite,
    surfaceVariant = PanelAlt,
    onSurfaceVariant = TextSoft,
    outline = StrokeDark,
    outlineVariant = StrokeSoft,
    error = DangerRed,
    onError = Color.White
)

@Composable
fun FakePingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AppTypography,
        content = content
    )
}
