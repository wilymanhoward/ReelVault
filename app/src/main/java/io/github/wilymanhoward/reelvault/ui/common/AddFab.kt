package io.github.wilymanhoward.reelvault.ui.common

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

/** The + button that opens into "Paste link" and "Type a link". */
@Composable
fun AddFab(onPaste: () -> Unit, onType: () -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val rotation by animateFloatAsState(
        if (open) 135f else 0f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "fabRotation",
    )
    BackHandler(enabled = open) { open = false }

    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MenuAction(visible = open, order = 1, label = "Type a link", icon = Icons.Outlined.Keyboard) { open = false; onType() }
        MenuAction(visible = open, order = 0, label = "Paste link", icon = Icons.Outlined.ContentPaste) { open = false; onPaste() }
        FloatingActionButton(
            onClick = {
                haptics.performHapticFeedback(if (open) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn)
                open = !open
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Icon(Icons.Filled.Add, contentDescription = if (open) "Close" else "Add a link", modifier = Modifier.rotate(rotation))
        }
    }
}

/** One menu entry; [order] 0 is nearest the button and appears first. */
@Composable
private fun MenuAction(visible: Boolean, order: Int, label: String, icon: ImageVector, onClick: () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(180, delayMillis = order * 50)) +
            scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy), initialScale = 0.7f, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 1f)) +
            slideInVertically(tween(220, delayMillis = order * 50)) { it / 2 },
        exit = fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.8f),
    ) {
        ExtendedFloatingActionButton(
            onClick = onClick,
            icon = { Icon(icon, null) },
            text = { Text(label) },
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}
