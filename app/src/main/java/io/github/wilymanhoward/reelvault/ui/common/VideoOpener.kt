package io.github.wilymanhoward.reelvault.ui.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import io.github.wilymanhoward.reelvault.domain.model.Platform

/** Native app packages per platform, in preference order (these also appear in the manifest's <queries>). */
private val PLATFORM_PACKAGES = mapOf(
    Platform.TIKTOK to listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill"),
    Platform.INSTAGRAM to listOf("com.instagram.android"),
    Platform.YOUTUBE to listOf("com.google.android.youtube"),
    Platform.FACEBOOK to listOf("com.facebook.katana"),
    Platform.X to listOf("com.twitter.android"),
    Platform.PINTEREST to listOf("com.pinterest"),
    Platform.XIAOHONGSHU to listOf("com.xingin.xhs"),
)

/**
 * Opens [url] in the platform's own app when it's installed, otherwise in a Custom Tab,
 * otherwise in whatever handles the link.
 */
fun openVideo(context: Context, url: String, platform: Platform) {
    val uri = url.toUri()
    val view = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    for (pkg in PLATFORM_PACKAGES[platform].orEmpty()) {
        try {
            context.startActivity(Intent(view).setPackage(pkg))
            return
        } catch (_: ActivityNotFoundException) {
            // Not installed (or doesn't handle this link); try the next option.
        }
    }
    try {
        CustomTabsIntent.Builder().setShowTitle(true).build()
            .apply { intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            .launchUrl(context, uri)
    } catch (_: ActivityNotFoundException) {
        try {
            context.startActivity(view)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "No app can open this link", Toast.LENGTH_SHORT).show()
        }
    }
}
