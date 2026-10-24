package io.github.wilymanhoward.reelvault.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.wilymanhoward.reelvault.R

// Apple-native type on Android. SF Pro and New York can't ship outside Apple platforms, so:
// Inter (closest open match to SF Pro) carries all interface text, and Source Serif 4 (in the role
// of New York) sets the large titles, keeping the album feel of the Once reference.
// Sizes follow iOS Dynamic Type at its default size (pt ≈ sp), so system font scaling still applies.
// Both are variable fonts (OFL, see licenses/); the optical size axis is matched to each style.

@OptIn(ExperimentalTextApi::class)
private fun inter(weight: Int, size: Float) = FontFamily(
    Font(
        R.font.inter,
        weight = FontWeight(weight),
        variationSettings = FontVariation.Settings(FontVariation.weight(weight), FontVariation.Setting("opsz", size.coerceIn(14f, 32f))),
    ),
)

@OptIn(ExperimentalTextApi::class)
private fun serif(weight: Int, size: Float) = FontFamily(
    Font(
        R.font.source_serif_4,
        weight = FontWeight(weight),
        variationSettings = FontVariation.Settings(FontVariation.weight(weight), FontVariation.Setting("opsz", size.coerceIn(8f, 60f))),
    ),
)

/** A serif title style: [size]/[height] in sp, tracked slightly tight as large serif titles want. */
private fun title(size: Int, height: Int, weight: Int = 600) = TextStyle(
    fontFamily = serif(weight, size.toFloat()),
    fontWeight = FontWeight(weight),
    fontSize = size.sp,
    lineHeight = height.sp,
    letterSpacing = (-0.01 * size).sp,
)

/** An Inter style. [tracking] mirrors SF Pro's size-specific tracking: tighter at body sizes, open at captions. */
private fun text(size: Int, height: Int, weight: Int = 400, tracking: Double) = TextStyle(
    fontFamily = inter(weight, size.toFloat()),
    fontWeight = FontWeight(weight),
    fontSize = size.sp,
    lineHeight = height.sp,
    letterSpacing = tracking.sp,
)

internal val ReelVaultTypography = Typography(
    // Onboarding and hero moments.
    displayLarge = title(52, 56, 600),
    displayMedium = title(44, 48, 600),
    displaySmall = title(38, 42, 600),
    // iOS Large Title (screen titles), Title 1, Title 2.
    headlineLarge = title(34, 41, 600),
    headlineMedium = title(28, 34, 600),
    headlineSmall = title(22, 28, 600),
    // iOS Title 3, Headline, Subheadline (semibold).
    titleLarge = text(20, 25, 600, tracking = -0.2),
    titleMedium = text(17, 22, 600, tracking = -0.3),
    titleSmall = text(15, 20, 600, tracking = -0.15),
    // iOS Body, Subheadline, Footnote.
    bodyLarge = text(17, 22, tracking = -0.3),
    bodyMedium = text(15, 20, tracking = -0.15),
    bodySmall = text(13, 18, tracking = -0.05),
    // Buttons (17 semibold like iOS), section headers and metadata, tab labels.
    labelLarge = text(17, 22, 600, tracking = -0.3),
    labelMedium = text(13, 18, 500, tracking = -0.05),
    labelSmall = text(11, 13, 500, tracking = 0.05),
)
