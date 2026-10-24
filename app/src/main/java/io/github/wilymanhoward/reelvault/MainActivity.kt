package io.github.wilymanhoward.reelvault

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.wilymanhoward.reelvault.ui.AppShell
import io.github.wilymanhoward.reelvault.ui.MainViewModel
import io.github.wilymanhoward.reelvault.ui.theme.ReelVaultTheme

/** The single activity hosting the whole Compose UI. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            ReelVaultTheme(themeMode = settings.themeMode, dynamicColor = settings.dynamicColor) {
                AppShell()
            }
        }
    }
}
