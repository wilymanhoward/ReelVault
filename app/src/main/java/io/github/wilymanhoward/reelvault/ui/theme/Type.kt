package io.github.wilymanhoward.reelvault.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.wilymanhoward.reelvault.R

// Bricolage Grotesque (variable, OFL; see licenses/) for titles only. Body text stays in the system
// face so captions in any script render well.
@OptIn(ExperimentalTextApi::class)
private fun bricolage(weight: Int, opticalSize: Float) = Font(
    R.font.bricolage_grotesque,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight), FontVariation.Setting("opsz", opticalSize)),
)

private val Display = FontFamily(bricolage(800, 72f))
private val Headline = FontFamily(bricolage(700, 36f))
private val Title = FontFamily(bricolage(600, 18f))

private val base = Typography()

internal val ReelVaultTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = Display, letterSpacing = (-0.03).em),
    displayMedium = base.displayMedium.copy(fontFamily = Display, letterSpacing = (-0.03).em),
    displaySmall = base.displaySmall.copy(fontFamily = Display, letterSpacing = (-0.02).em),
    headlineLarge = TextStyle(fontFamily = Display, fontSize = 34.sp, lineHeight = 36.sp, letterSpacing = (-0.025).em),
    headlineMedium = TextStyle(fontFamily = Headline, fontSize = 28.sp, lineHeight = 32.sp, letterSpacing = (-0.02).em),
    headlineSmall = TextStyle(fontFamily = Headline, fontSize = 24.sp, lineHeight = 28.sp, letterSpacing = (-0.015).em),
    titleLarge = TextStyle(fontFamily = Title, fontSize = 22.sp, lineHeight = 26.sp, letterSpacing = (-0.01).em),
    titleMedium = TextStyle(fontFamily = Title, fontSize = 17.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = Title, fontSize = 15.sp, lineHeight = 20.sp),
    bodyLarge = base.bodyLarge,
    bodyMedium = base.bodyMedium,
    bodySmall = base.bodySmall,
    labelLarge = base.labelLarge,
    labelMedium = base.labelMedium,
    labelSmall = base.labelSmall,
)
