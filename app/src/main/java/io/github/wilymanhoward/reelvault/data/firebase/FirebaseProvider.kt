package io.github.wilymanhoward.reelvault.data.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Entry point to Firebase that copes with builds made without `google-services.json`
 * (forks, CI): then [isConfigured] is false and everything Firebase-related stays off.
 */
@Singleton
class FirebaseProvider @Inject constructor(@ApplicationContext private val context: Context) {

    /** True when the build carries a Firebase config and FirebaseApp initialized from it. */
    val isConfigured: Boolean by lazy { FirebaseApp.getApps(context).isNotEmpty() }

    val auth: FirebaseAuth? by lazy { if (isConfigured) FirebaseAuth.getInstance() else null }

    val firestore: FirebaseFirestore? by lazy {
        if (!isConfigured) return@lazy null
        FirebaseFirestore.getInstance().apply {
            // Room is the app's offline store; a second persistent cache in Firestore would only duplicate it.
            runCatching {
                firestoreSettings = FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                    .build()
            }.onFailure { Log.w("ReelVault", "Firestore settings already applied", it) }
        }
    }

    /**
     * OAuth web client id generated into resources by the google-services plugin, or null.
     * Looked up by name because the resource only exists in builds that have the Firebase config.
     */
    val webClientId: String? by lazy {
        @Suppress("DiscouragedApi")
        val id = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (id == 0) null else context.getString(id)
    }
}
