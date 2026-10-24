package io.github.wilymanhoward.reelvault.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import io.github.wilymanhoward.reelvault.domain.model.ThemeMode

private val LightColors = lightColorScheme(
    primary = Coral,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDADF),
    onPrimaryContainer = CoralDeep,
    secondary = Violet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6E1FF),
    onSecondaryContainer = Color(0xFF2A1F8F),
    tertiary = Amber,
    onTertiary = TextOnPaper,
    background = Paper,
    onBackground = TextOnPaper,
    surface = Paper,
    onSurface = TextOnPaper,
    surfaceVariant = PaperSunk,
    onSurfaceVariant = TextOnPaperMuted,
    surfaceContainerLowest = PaperRaised,
    surfaceContainerLow = Color(0xFFF8F3EF),
    surfaceContainer = Color(0xFFF4EEEA),
    surfaceContainerHigh = PaperSunk,
    surfaceContainerHighest = Color(0xFFEAE3DD),
    outline = Color(0xFFCFC6CB),
    outlineVariant = Color(0xFFE4DCD8),
)

private val DarkColors = darkColorScheme(
    primary = CoralSoft,
    onPrimary = Color(0xFF4A0816),
    primaryContainer = CoralDeep,
    onPrimaryContainer = Color(0xFFFFDADF),
    secondary = Color(0xFFB7AEFF),
    onSecondary = Color(0xFF221785),
    secondaryContainer = Color(0xFF3B2FB0),
    onSecondaryContainer = Color(0xFFE6E1FF),
    tertiary = Amber,
    onTertiary = Ink,
    background = Ink,
    onBackground = TextOnInk,
    surface = Ink,
    onSurface = TextOnInk,
    surfaceVariant = InkHigh,
    onSurfaceVariant = TextOnInkMuted,
    surfaceContainerLowest = Color(0xFF0B0A10),
    surfaceContainerLow = Color(0xFF16131E),
    surfaceContainer = InkRaised,
    surfaceContainerHigh = InkHigh,
    surfaceContainerHighest = Color(0xFF302B3A),
    outline = Color(0xFF4A4454),
    outlineVariant = Color(0xFF332E3C),
)

/**
 * ReelVault's Material 3 theme.
 *
 * @param themeMode light, dark, or follow the system setting.
 * @param dynamicColor use Material You colors on Android 12+; the brand palette otherwise.
 */
@Composable
fun ReelVaultTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        dark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, typography = ReelVaultTypography, content = content)
}
