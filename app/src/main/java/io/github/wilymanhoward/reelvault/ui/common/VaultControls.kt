package io.github.wilymanhoward.reelvault.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
fun VaultIconButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Box(
        modifier.size(48.dp).pressScale(interaction).clip(RoundedCornerShape(17.dp))
            .then(if (focused) Modifier.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(17.dp)) else Modifier)
            .clickable(interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.35f)) { content() }
    }
}

@Composable
private fun Action(
    onClick: () -> Unit, modifier: Modifier, enabled: Boolean, fill: Color, ink: Color,
    outlined: Boolean = false, content: @Composable RowScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(17.dp)
    Row(
        modifier.defaultMinSize(minHeight = 48.dp).pressScale(interaction).clip(shape)
            .background(fill.copy(alpha = fill.alpha * if (enabled) 1f else 0.35f))
            .then(if (focused || outlined) Modifier.border(1.dp, if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, shape) else Modifier)
            .clickable(interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides ink.copy(alpha = if (enabled) 1f else 0.35f)) {
            ProvideTextStyle(MaterialTheme.typography.labelLarge) { content() }
        }
    }
}

@Composable
fun VaultButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) =
    Action(onClick, modifier, enabled, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary, content = content)

@Composable
fun VaultOutlinedButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) =
    Action(onClick, modifier, enabled, Color.Transparent, MaterialTheme.colorScheme.onSurface, outlined = true, content = content)

@Composable
fun VaultTextButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) =
    Action(onClick, modifier, enabled, Color.Transparent, MaterialTheme.colorScheme.primary, content = content)

@Composable
fun VaultFilledTonalButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) =
    Action(onClick, modifier, enabled, MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurface, content = content)

@Composable
fun VaultExtendedFloatingActionButton(
    onClick: () -> Unit, icon: @Composable () -> Unit, text: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
) {
    Action(onClick, modifier, true, containerColor, contentColor) {
        icon()
        Box(Modifier.padding(start = 10.dp)) { text() }
    }
}

@Composable
fun VaultTopAppBar(
    title: @Composable () -> Unit, modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {}, actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp).heightIn(min = 52.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        navigationIcon()
        Box(Modifier.weight(1f)) {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
                ProvideTextStyle(MaterialTheme.typography.headlineLarge) { title() }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

@Composable
fun VaultLargeTopAppBar(
    title: @Composable () -> Unit, modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {}, actions: @Composable RowScope.() -> Unit = {},
) = VaultTopAppBar(title, modifier, navigationIcon, actions)

@Composable
fun VaultFilterChip(
    selected: Boolean, onClick: () -> Unit, label: @Composable () -> Unit,
    modifier: Modifier = Modifier, leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val interaction = interactionSource ?: remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(10.dp)
    val fill by animateColorAsState(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer)
    Row(
        modifier.heightIn(min = 44.dp).padding(vertical = 4.dp).clip(shape).background(fill)
            .then(if (focused) Modifier.border(1.dp, MaterialTheme.colorScheme.primary, shape) else Modifier)
            .selectable(selected, interaction, indication = null, role = Role.Checkbox, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) {
            ProvideTextStyle(MaterialTheme.typography.labelMedium) {
                leadingIcon?.invoke(); label(); trailingIcon?.invoke()
            }
        }
    }
}

@Composable
fun VaultInputChip(selected: Boolean, onClick: () -> Unit, label: @Composable () -> Unit, modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null, trailingIcon: (@Composable () -> Unit)? = null) =
    VaultFilterChip(selected, onClick, label, modifier, leadingIcon, trailingIcon)

@Composable
fun VaultAssistChip(onClick: () -> Unit, label: @Composable () -> Unit, modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null, trailingIcon: (@Composable () -> Unit)? = null) =
    VaultFilterChip(false, onClick, label, modifier, leadingIcon, trailingIcon)

@Composable
fun VaultSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    // The off track uses `outline` (3:1+ against the row in both themes) so an off switch stays visible.
    val fill by animateColorAsState(if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
    Box(
        modifier.size(48.dp).then(if (onCheckedChange != null) Modifier.toggleable(checked, interaction,
            indication = null, role = Role.Switch, onValueChange = onCheckedChange) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(48.dp, 31.dp)
                .then(if (focused) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
                .padding(3.dp),
        )
        Box(Modifier.size(42.dp, 25.dp).clip(CircleShape).background(fill).padding(3.dp)) {
            val offset by animateDpAsState(if (checked) 17.dp else 0.dp)
            Box(Modifier.padding(start = offset).size(19.dp).background(MaterialTheme.colorScheme.background, CircleShape))
        }
    }
}

@Composable
fun <T> VaultChoices(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(8.dp).clip(RoundedCornerShape(17.dp)).background(MaterialTheme.colorScheme.background).padding(4.dp)) {
        options.forEach { (value, label) ->
            val active = selected == value
            val interaction = remember { MutableInteractionSource() }
            val focused by interaction.collectIsFocusedAsState()
            Box(
                Modifier.weight(1f).heightIn(min = 44.dp).clip(RoundedCornerShape(9.dp))
                    .background(if (active) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent)
                    .then(if (focused) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(9.dp)) else Modifier)
                    .selectable(active, interaction, indication = null, role = Role.RadioButton, onClick = { onSelect(value) }),
                contentAlignment = Alignment.Center,
            ) { Text(label, style = MaterialTheme.typography.labelMedium, color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
fun VaultTab(selected: Boolean, onClick: () -> Unit, icon: @Composable () -> Unit, label: String, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val fill by animateColorAsState(if (selected) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent)
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(fill)
            .then(if (focused) Modifier.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp)) else Modifier)
            .selectable(selected, interaction, indication = null, role = Role.Tab, onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        CompositionLocalProvider(LocalContentColor provides if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) {
            icon()
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}
