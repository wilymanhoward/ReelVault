package io.github.wilymanhoward.reelvault.ui.theme

import androidx.compose.ui.graphics.Color
import io.github.wilymanhoward.reelvault.domain.model.Platform

/**
 * How a platform looks on cards: gradient colors for badges and fallback art, a short monogram,
 * and the card aspect ratio (width / height) that suits its content.
 */
data class PlatformStyle(val colors: List<Color>, val monogram: String, val aspectRatio: Float)

val Platform.style: PlatformStyle
    get() = when (this) {
        Platform.TIKTOK -> PlatformStyle(listOf(Color(0xFF25F4EE), Color(0xFF111111), Color(0xFFFE2C55)), "TT", 3f / 4f)
        Platform.INSTAGRAM -> PlatformStyle(listOf(Color(0xFFF58529), Color(0xFFDD2A7B), Color(0xFF8134AF)), "IG", 3f / 4f)
        Platform.YOUTUBE -> PlatformStyle(listOf(Color(0xFFFF3D3D), Color(0xFFB0001E)), "YT", 3f / 4f)
        Platform.FACEBOOK -> PlatformStyle(listOf(Color(0xFF3B8BFF), Color(0xFF0A4FB3)), "f", 1f)
        Platform.X -> PlatformStyle(listOf(Color(0xFF3A3F47), Color(0xFF0E0F11)), "X", 1f)
        Platform.PINTEREST -> PlatformStyle(listOf(Color(0xFFFF3B5C), Color(0xFFA3001A)), "P", 2f / 3f)
        Platform.XIAOHONGSHU -> PlatformStyle(listOf(Color(0xFFFF6A80), Color(0xFFE0102F)), "RED", 3f / 4f)
        Platform.OTHER -> PlatformStyle(listOf(Accent, Coral), "↗", 1f)
    }
