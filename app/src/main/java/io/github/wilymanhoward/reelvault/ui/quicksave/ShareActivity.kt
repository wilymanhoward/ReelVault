package io.github.wilymanhoward.reelvault.ui.quicksave

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.wilymanhoward.reelvault.domain.model.AppSettings
import io.github.wilymanhoward.reelvault.ui.MainViewModel
import io.github.wilymanhoward.reelvault.ui.theme.ReelVaultTheme

/**
 * Receives `ACTION_SEND` text from other apps' share sheets and shows the quick-save sheet over them.
 * Runs in its own task, so finishing it drops the user straight back into the app they shared from.
 */
@AndroidEntryPoint
class ShareActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by mainViewModel.settings.collectAsStateWithLifecycle()
            val loaded = settings ?: AppSettings()
            ReelVaultTheme(themeMode = loaded.themeMode) {
                QuickSaveSheet(onClose = ::finish)
            }
        }
    }
}
