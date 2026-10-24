package io.github.wilymanhoward.reelvault.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

// The rest of the Vault system: the pieces screens used to take straight from Material (alerts,
// fields, menus, list rows, snackbars), reshaped to the iOS conventions in DESIGN.md. Parameter
// names match Material's, so screens switch by import alias without changing their call sites.

/**
 * An iOS-style alert: compact rounded panel, centered title and message, actions stacked full width
 * below (primary on top). Escape / back and tapping outside call [onDismissRequest].
 */
@Composable
fun VaultAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = modifier.widthIn(max = 340.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Column(Modifier.padding(top = 24.dp, start = 20.dp, end = 20.dp, bottom = 12.dp)) {
                title?.let {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        ProvideTextStyle(MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center)) { it() }
                    }
                }
                text?.let {
                    Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                            ProvideTextStyle(MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center)) { it() }
                        }
                    }
                }
                Column(
                    Modifier.fillMaxWidth().padding(top = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    confirmButton()
                    dismissButton?.invoke()
                }
            }
        }
    }
}

/**
 * A text field on the Vault surface. Its border uses `outline`, which holds 3:1 against every surface
 * it sits on in both themes, and turns to the action color with a 2dp stroke while focused.
 */
@Composable
fun VaultTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: (@Composable () -> Unit)? = null,
    placeholder: (@Composable () -> Unit)? = null,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val colors = MaterialTheme.colorScheme
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        textStyle = MaterialTheme.typography.bodyLarge,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.primary,
            unfocusedBorderColor = colors.outline,
            focusedLabelColor = colors.onSurface,
            unfocusedLabelColor = colors.onSurfaceVariant,
            cursorColor = colors.primary,
            focusedContainerColor = colors.surfaceContainerHigh,
            unfocusedContainerColor = colors.surfaceContainerHigh,
        ),
    )
}

/** A menu on a raised rounded panel; its shadow marks it as floating above the screen it came from. */
@Composable
fun VaultDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp,
        shadowElevation = 12.dp,
        content = content,
    )
}

/**
 * A grouped-list row in the iOS style: 44dp minimum, title in body size, an optional secondary line in
 * footnote size, and trailing accessories. Rows inside a group are separated by [VaultSeparator].
 */
@Composable
fun VaultListItem(
    headlineContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    overlineContent: (@Composable () -> Unit)? = null,
    supportingContent: (@Composable () -> Unit)? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        leadingContent?.let {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) { it() }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            overlineContent?.let {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                    ProvideTextStyle(MaterialTheme.typography.labelMedium) { it() }
                }
            }
            ProvideTextStyle(MaterialTheme.typography.bodyLarge) { headlineContent() }
            supportingContent?.let {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                    ProvideTextStyle(MaterialTheme.typography.bodySmall) { it() }
                }
            }
        }
        trailingContent?.let { trailing ->
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                Row(verticalAlignment = Alignment.CenterVertically, content = trailing)
            }
        }
    }
}

/** The hairline between rows of a group, inset from the leading edge like iOS. */
@Composable
fun VaultSeparator(inset: Boolean = true) {
    HorizontalDivider(
        modifier = Modifier.padding(start = if (inset) 16.dp else 0.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

/** Snackbars as a rounded inverse panel above the tab bar, action in the panel's own accent. */
@Composable
fun VaultSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(hostState, modifier) { data ->
        Snackbar(
            snackbarData = data,
            modifier = Modifier.padding(horizontal = 12.dp),
            shape = RoundedCornerShape(14.dp),
            containerColor = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            actionColor = MaterialTheme.colorScheme.inversePrimary,
        )
    }
}

/** A plain informative line for grouped settings sections (iOS group footer). */
@Composable
fun GroupFooter(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 8.dp),
    )
}
