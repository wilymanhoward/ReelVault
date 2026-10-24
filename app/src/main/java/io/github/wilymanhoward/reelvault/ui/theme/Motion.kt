package io.github.wilymanhoward.reelvault.ui.theme

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * True when the user turned animations off (system animator duration scale = 0). Compose's own
 * animations already obey that scale; this flag covers what it can't see: delays, staggered
 * entrances, looping decoration and auto-advancing UI.
 */
val LocalReducedMotion = staticCompositionLocalOf { false }

/** Reads the system animation setting. Call once near the root and provide it via [LocalReducedMotion]. */
@Composable
@ReadOnlyComposable
fun systemReducedMotion(): Boolean =
    Settings.Global.getFloat(LocalContext.current.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
