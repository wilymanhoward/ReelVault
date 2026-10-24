package io.github.wilymanhoward.reelvault.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import android.app.Activity
import io.github.wilymanhoward.reelvault.domain.model.ThemeMode

private val LightColors = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    inversePrimary = AccentLight,
    primaryContainer = Color(0xFFE6E3DC),
    onPrimaryContainer = AccentDeep,
    secondary = Color(0xFF48484A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE5E5EA),
    onSecondaryContainer = InkText,
    tertiary = Coral,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDDE1),
    onTertiaryContainer = Color(0xFF7A0A1C),
    background = Fog,
    onBackground = InkText,
    surface = FogRaised,
    onSurface = InkText,
    surfaceVariant = FogSunk,
    onSurfaceVariant = InkMuted,
    surfaceContainerLowest = FogRaised,
    surfaceContainerLow = Color(0xFFF5F3EF),
    surfaceContainer = FogRaised,
    surfaceContainerHigh = Color(0xFFEAE7E1),
    surfaceContainerHighest = FogSunk,
    // Control boundaries (field borders, the off switch track) must hold 3:1 on every light surface:
    // #86837D is 3.78:1 on white, 3.35:1 on the page, 3.06:1 on the raised fill.
    outline = Color(0xFF86837D),
    // Decorative dividers only; never the sole boundary of a control.
    outlineVariant = Color(0xFFD1D1D6),
)

private val DarkColors = darkColorScheme(
    primary = AccentLight,
    onPrimary = Color(0xFF171715),
    inversePrimary = Accent,
    primaryContainer = Color(0xFF2D2C29),
    onPrimaryContainer = Color(0xFFE6E3DC),
    secondary = Color(0xFFD5D2CB),
    onSecondary = Color(0xFF2C2C2E),
    secondaryContainer = VaultHigh,
    onSecondaryContainer = LightText,
    tertiary = CoralSoft,
    onTertiary = Color(0xFF5A0012),
    tertiaryContainer = Color(0xFF8C1C2E),
    onTertiaryContainer = Color(0xFFFFDDE1),
    background = Vault,
    onBackground = LightText,
    surface = VaultRaised,
    onSurface = LightText,
    surfaceVariant = VaultHigh,
    onSurfaceVariant = LightMuted,
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF141414),
    surfaceContainer = VaultRaised,
    surfaceContainerHigh = VaultHigh,
    surfaceContainerHighest = Color(0xFF313130),
    outline = Color(0xFF77736D),
    outlineVariant = Color(0xFF33322F),
)

/**
 * ReelVault's fixed studio palette. Theme changes cross-fade without changing layout.
 *
 * @param themeMode light, dark, or follow the system setting.
 */
@Composable
fun ReelVaultTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val target = if (dark) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as? Activity)?.window?.let { window ->
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
        }
    }
    CompositionLocalProvider(LocalReducedMotion provides systemReducedMotion()) {
        MaterialTheme(colorScheme = animateColorScheme(target), typography = ReelVaultTypography,
            shapes = Shapes(small = RoundedCornerShape(10.dp), medium = RoundedCornerShape(14.dp), large = RoundedCornerShape(16.dp), extraLarge = RoundedCornerShape(22.dp)),
            content = content)
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
