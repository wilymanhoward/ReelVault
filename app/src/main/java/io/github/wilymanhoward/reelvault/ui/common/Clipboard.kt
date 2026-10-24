package io.github.wilymanhoward.reelvault.ui.common

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo

/** The clipboard's current text, or null. Android 12+ shows a "pasted" notice when this reads. */
fun readClipboardText(context: Context): String? {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return null
    return clipboard.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString()
}

/**
 * Calls [onText] with the clipboard text whenever the window gains focus (Android only allows
 * clipboard reads while focused) and the clip changed since the last read. The change check uses
 * the clip description, which doesn't trigger the system "pasted" notice.
 */
@Composable
fun ClipboardTextEffect(onText: (String) -> Unit) {
    val context = LocalContext.current
    val focused = LocalWindowInfo.current.isWindowFocused
    var lastTimestamp by rememberSaveable { mutableLongStateOf(-1L) }
    LaunchedEffect(focused) {
        if (!focused) return@LaunchedEffect
        val description = context.getSystemService(ClipboardManager::class.java)?.primaryClipDescription
            ?: return@LaunchedEffect
        if (description.timestamp == lastTimestamp) return@LaunchedEffect
        lastTimestamp = description.timestamp
        if (!description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) &&
            !description.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML)
        ) return@LaunchedEffect
        readClipboardText(context)?.let(onText)
    }
}
