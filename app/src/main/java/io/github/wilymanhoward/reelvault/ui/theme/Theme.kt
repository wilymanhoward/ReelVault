package io.github.wilymanhoward.reelvault.ui.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import io.github.wilymanhoward.reelvault.domain.model.ThemeMode

private val LightColors = lightColorScheme(
    primary = Ultraviolet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5DFFF),
    onPrimaryContainer = UltravioletDeep,
    secondary = Color(0xFF4A4658),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE4E2EC),
    onSecondaryContainer = InkText,
    tertiary = Coral,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDDE1),
    onTertiaryContainer = Color(0xFF7A0A1C),
    background = Fog,
    onBackground = InkText,
    surface = Fog,
    onSurface = InkText,
    surfaceVariant = FogSunk,
    onSurfaceVariant = InkMuted,
    surfaceContainerLowest = FogRaised,
    surfaceContainerLow = Color(0xFFF8F8FB),
    surfaceContainer = FogRaised,
    surfaceContainerHigh = Color(0xFFECEDF3),
    surfaceContainerHighest = FogSunk,
    outline = Color(0xFFC5C3CF),
    outlineVariant = Color(0xFFDDDCE4),
)

private val DarkColors = darkColorScheme(
    primary = UltravioletSoft,
    onPrimary = Color(0xFF1E0E7A),
    primaryContainer = Color(0xFF3A27A8),
    onPrimaryContainer = Color(0xFFE5DFFF),
    secondary = Color(0xFFC9C5D6),
    onSecondary = Color(0xFF2B2836),
    secondaryContainer = VaultHigh,
    onSecondaryContainer = LightText,
    tertiary = CoralSoft,
    onTertiary = Color(0xFF5A0012),
    tertiaryContainer = Color(0xFF8C1C2E),
    onTertiaryContainer = Color(0xFFFFDDE1),
    background = Vault,
    onBackground = LightText,
    surface = Vault,
    onSurface = LightText,
    surfaceVariant = VaultHigh,
    onSurfaceVariant = LightMuted,
    surfaceContainerLowest = Color(0xFF0A0910),
    surfaceContainerLow = Color(0xFF15131D),
    surfaceContainer = VaultRaised,
    surfaceContainerHigh = VaultHigh,
    surfaceContainerHighest = Color(0xFF2F2B3B),
    outline = Color(0xFF4C4859),
    outlineVariant = Color(0xFF34303F),
)

/**
 * ReelVault's Material 3 theme. Switching light/dark (or turning wallpaper colors on) cross-fades
 * the colors instead of snapping.
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
    val target = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        dark -> DarkColors
        else -> LightColors
    }
    CompositionLocalProvider(LocalReducedMotion provides systemReducedMotion()) {
        MaterialTheme(colorScheme = animateColorScheme(target), typography = ReelVaultTypography, content = content)
    }
}

/** Animates every color role toward [target], so theme switches cross-fade. */
@Composable
private fun animateColorScheme(target: ColorScheme): ColorScheme {
    @Composable
    fun Color.animated(): Color {
        val value by animateColorAsState(this, tween(durationMillis = 450), label = "themeColor")
        return value
    }
    return target.copy(
        primary = target.primary.animated(),
        onPrimary = target.onPrimary.animated(),
        primaryContainer = target.primaryContainer.animated(),
        onPrimaryContainer = target.onPrimaryContainer.animated(),
        secondary = target.secondary.animated(),
        onSecondary = target.onSecondary.animated(),
        secondaryContainer = target.secondaryContainer.animated(),
        onSecondaryContainer = target.onSecondaryContainer.animated(),
        tertiary = target.tertiary.animated(),
        onTertiary = target.onTertiary.animated(),
        tertiaryContainer = target.tertiaryContainer.animated(),
        onTertiaryContainer = target.onTertiaryContainer.animated(),
        background = target.background.animated(),
        onBackground = target.onBackground.animated(),
        surface = target.surface.animated(),
        onSurface = target.onSurface.animated(),
        surfaceVariant = target.surfaceVariant.animated(),
        onSurfaceVariant = target.onSurfaceVariant.animated(),
        surfaceContainerLowest = target.surfaceContainerLowest.animated(),
        surfaceContainerLow = target.surfaceContainerLow.animated(),
        surfaceContainer = target.surfaceContainer.animated(),
        surfaceContainerHigh = target.surfaceContainerHigh.animated(),
        surfaceContainerHighest = target.surfaceContainerHighest.animated(),
        outline = target.outline.animated(),
        outlineVariant = target.outlineVariant.animated(),
        error = target.error.animated(),
        errorContainer = target.errorContainer.animated(),
        onErrorContainer = target.onErrorContainer.animated(),
    )
}
