package io.github.wilymanhoward.reelvault.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import io.github.wilymanhoward.reelvault.ui.common.VaultListItem as ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import io.github.wilymanhoward.reelvault.ui.common.VaultTopAppBar as TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.wilymanhoward.reelvault.BuildConfig
import io.github.wilymanhoward.reelvault.domain.model.Platform
import io.github.wilymanhoward.reelvault.ui.common.BackButton
import io.github.wilymanhoward.reelvault.ui.common.VaultMark
import io.github.wilymanhoward.reelvault.ui.common.openVideo

private const val REPO_URL = "https://github.com/wilymanhoward/ReelVault"

private data class Credit(val name: String, val license: String, val url: String)

// Main open-source components. Each one's full license text ships with its artifact.
private val credits = listOf(
    Credit("Jetpack Compose, Navigation, Room, WorkManager, DataStore, Hilt", "Apache 2.0", "https://developer.android.com/jetpack"),
    Credit("Kotlin, kotlinx.coroutines, kotlinx.serialization", "Apache 2.0", "https://kotlinlang.org"),
    Credit("OkHttp, Retrofit", "Apache 2.0", "https://square.github.io/okhttp/"),
    Credit("Coil", "Apache 2.0", "https://coil-kt.github.io/coil/"),
    Credit("jsoup", "MIT", "https://jsoup.org"),
    Credit("Firebase Android SDK", "Apache 2.0", "https://github.com/firebase/firebase-android-sdk"),
    Credit("ML Kit image labeling", "ML Kit Terms of Service", "https://developers.google.com/ml-kit/terms"),
    Credit("Inter typeface", "SIL Open Font License 1.1", "https://github.com/rsms/inter"),
    Credit("Source Serif 4 typeface", "SIL Open Font License 1.1", "https://github.com/adobe-fonts/source-serif"),
)

/** Version, source link and open-source licenses. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val open: (String) -> Unit = { openVideo(context, it, Platform.OTHER) }
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = { TopAppBar(title = { Text("About") }, navigationIcon = { BackButton(onBack) }) },
    ) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                Column(
                    Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    VaultMark(size = 88.dp)
                    Text("ReelVault", style = MaterialTheme.typography.headlineLarge)
                    Text("Version ${BuildConfig.VERSION_NAME}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "Keeps the short videos you save on every platform in one place. Free and open source under the MIT license.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            item {
                ListItem(
                    modifier = Modifier.clickable { open(REPO_URL) },
                    headlineContent = { Text("Source code") },
                    supportingContent = { Text(REPO_URL.removePrefix("https://")) },
                )
            }
            item { SectionTitle("Open-source licenses") }
            items(credits) { credit ->
                ListItem(
                    modifier = Modifier.clickable { open(credit.url) },
                    headlineContent = { Text(credit.name) },
                    supportingContent = { Text(credit.license) },
                )
            }
        }
    }
}
