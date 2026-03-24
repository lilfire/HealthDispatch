package com.healthdispatch.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.healthdispatch.sync.SyncScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var syncScheduler: SyncScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            syncScheduler.startForegroundService()
            syncScheduler.startPeriodicSync()
        }
    }
}
