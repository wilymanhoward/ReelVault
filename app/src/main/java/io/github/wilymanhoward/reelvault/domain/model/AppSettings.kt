package io.github.wilymanhoward.reelvault.domain.model

/** Which color scheme the app uses. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** How the Home screen lays out saves. */
enum class ViewMode { GRID, LIST }

/**
 * User preferences stored on the device.
 *
 * @property dynamicColor legacy preference retained for backup compatibility; the studio palette is fixed.
 * @property imageLabeling run ML Kit on-device image labeling on thumbnails to help auto-tagging.
 */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val dynamicColor: Boolean = false,
    val imageLabeling: Boolean = true,
    val sortOrder: SortOrder = SortOrder.NEWEST,
    val viewMode: ViewMode = ViewMode.GRID,
    val onboardingDone: Boolean = false,
)
