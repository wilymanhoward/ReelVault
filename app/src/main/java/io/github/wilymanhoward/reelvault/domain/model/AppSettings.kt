package io.github.wilymanhoward.reelvault.domain.model

/** Which color scheme the app uses. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** How the Home screen lays out saves. */
enum class ViewMode { GRID, LIST }

/**
 * User preferences stored on the device.
 *
 * @property dynamicColor use Material You wallpaper colors instead of the ReelVault palette (Android 12+).
 * @property imageLabeling run ML Kit on-device image labeling on thumbnails to help auto-tagging.
 */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val imageLabeling: Boolean = true,
    val sortOrder: SortOrder = SortOrder.NEWEST,
    val viewMode: ViewMode = ViewMode.GRID,
)
