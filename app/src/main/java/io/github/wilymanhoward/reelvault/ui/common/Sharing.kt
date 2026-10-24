package io.github.wilymanhoward.reelvault.ui.common

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import io.github.wilymanhoward.reelvault.domain.model.Platform

/** Opens the system share sheet with [text]. */
fun shareText(context: Context, text: String, title: String? = null) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    title?.let { send.putExtra(Intent.EXTRA_TITLE, it) }
    context.startActivity(Intent.createChooser(send, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

/** Shares a single save: its original video link with a short note. */
fun shareVideo(context: Context, url: String, platform: Platform) {
    val where = if (platform == Platform.OTHER) "" else " on ${platform.displayName}"
    shareText(context, "Saw this$where, via ReelVault\n$url")
}

/** Copies [text] to the clipboard. Android 13+ shows its own confirmation. */
fun copyText(context: Context, label: String, text: String) {
    context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(label, text))
}
