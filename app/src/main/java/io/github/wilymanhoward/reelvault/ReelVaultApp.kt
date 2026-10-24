package io.github.wilymanhoward.reelvault

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import io.github.wilymanhoward.reelvault.data.sync.SyncTrigger
import javax.inject.Inject

/** Application entry point; sets up Hilt, Hilt-aware WorkManager and sync-on-change. */
@HiltAndroidApp
class ReelVaultApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var syncTrigger: SyncTrigger

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        syncTrigger.start()
    }
}
