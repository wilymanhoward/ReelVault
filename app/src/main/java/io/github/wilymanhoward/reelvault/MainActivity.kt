package io.github.wilymanhoward.reelvault

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.wilymanhoward.reelvault.data.share.shareIdFromLink
import io.github.wilymanhoward.reelvault.ui.AppShell
import io.github.wilymanhoward.reelvault.ui.MainViewModel
import io.github.wilymanhoward.reelvault.ui.theme.ReelVaultTheme
import kotlinx.coroutines.flow.MutableStateFlow

/** The single activity hosting the whole Compose UI. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    // Collection links are routed here rather than through Navigation's automatic deep linking,
    // which restarts the task for NEW_TASK intents and loses the link under singleTask.
    private val pendingShareId = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate so the splash theme hands over to the app theme.
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Only a fresh launch: after a config change the shared screen is already on the back stack.
        if (savedInstanceState == null) pendingShareId.value = shareIdOf(intent)
        // Keep the system splash up until settings are read (usually a few ms).
        splash.setKeepOnScreenCondition { viewModel.settings.value == null }
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val shareId by pendingShareId.collectAsStateWithLifecycle()
            val loaded = settings ?: return@setContent
            ReelVaultTheme(themeMode = loaded.themeMode) {
                AppShell(
                    onboardingDone = loaded.onboardingDone,
                    onFinishOnboarding = { viewModel.finishOnboarding() },
                    pendingShareId = shareId,
                    onShareIdHandled = { pendingShareId.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        shareIdOf(intent)?.let { pendingShareId.value = it }
    }

    private fun shareIdOf(intent: Intent?): String? =
        intent?.takeIf { it.action == Intent.ACTION_VIEW }?.dataString?.let { shareIdFromLink(it, BuildConfig.SHARE_HOST) }
}
