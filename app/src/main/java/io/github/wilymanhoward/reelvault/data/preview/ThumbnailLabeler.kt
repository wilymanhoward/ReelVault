package io.github.wilymanhoward.reelvault.data.preview

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** ML Kit's bundled on-device image labeler (no network, no API key) run on cached thumbnails. */
@Singleton
class ThumbnailLabeler @Inject constructor(@ApplicationContext private val context: Context) {

    private val labeler by lazy {
        ImageLabeling.getClient(ImageLabelerOptions.Builder().setConfidenceThreshold(MIN_CONFIDENCE).build())
    }

    /** Labels such as "Food" or "Beach" for the image at [path]; empty when it can't be read. */
    suspend fun labels(path: String): List<String> = runCatching {
        val image = InputImage.fromFilePath(context, Uri.fromFile(File(path)))
        labeler.process(image).await().map { it.text }
    }.getOrDefault(emptyList())

    private companion object {
        // High enough that "Sleeve" or "Font" on a busy thumbnail doesn't drive tags.
        const val MIN_CONFIDENCE = 0.75f
    }
}
