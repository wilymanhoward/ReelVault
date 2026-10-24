package io.github.wilymanhoward.reelvault.domain.model

/** Which color scheme the app uses. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * User preferences stored on the device.
 *
 * @property dynamicColor use Material You wallpaper colors instead of the ReelVault palette (Android 12+).
 */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
)
