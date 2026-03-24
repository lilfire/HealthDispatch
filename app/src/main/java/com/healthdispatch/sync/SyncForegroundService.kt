package com.healthdispatch.sync

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.healthdispatch.HealthDispatchApp
import com.healthdispatch.R
import com.healthdispatch.data.healthconnect.HealthConnectRepository
import com.healthdispatch.data.local.PendingSyncDao
import com.healthdispatch.data.local.PendingSyncRecord
import com.healthdispatch.data.local.SyncTokenDao
import com.healthdispatch.data.local.SyncToken
import com.healthdispatch.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.health.connect.client.changes.UpsertionChange
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

@AndroidEntryPoint
class SyncForegroundService : LifecycleService() {

    @Inject lateinit var healthConnectRepo: HealthConnectRepository
    @Inject lateinit var pendingSyncDao: PendingSyncDao
    @Inject lateinit var syncTokenDao: SyncTokenDao

    override fun onCreate() {
        super.onCreate()
        startForeground(
            NOTIFICATION_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        lifecycleScope.launch {
            while (isActive) {
                try {
                    pollHealthConnect()
                } catch (e: Exception) {
                    Log.e(TAG, "Poll cycle failed", e)
                }
                delay(POLL_INTERVAL_MS)
            }
        }

        return START_STICKY
    }

    private suspend fun pollHealthConnect() {
        if (!healthConnectRepo.hasAllPermissions()) return

        for (recordType in healthConnectRepo.supportedRecordTypes) {
            val typeName = recordType.simpleName ?: continue
            val existingToken = syncTokenDao.getToken(typeName)

            if (existingToken == null) {
                // First run: get initial token, skip existing data
                val token = healthConnectRepo.getChangesToken(setOf(recordType))
                syncTokenDao.upsert(SyncToken(recordType = typeName, token = token))
                continue
            }

            val result = healthConnectRepo.getChanges(existingToken.token)
            val upserts = result.changes.filterIsInstance<UpsertionChange>()

            if (upserts.isNotEmpty()) {
                val pendingRecords = upserts.map { change ->
                    PendingSyncRecord(
                        recordType = typeName,
                        recordId = change.record.metadata.id,
                        jsonPayload = serializeRecord(change.record)
                    )
                }
                pendingSyncDao.insertAll(pendingRecords)
            }

            syncTokenDao.upsert(SyncToken(recordType = typeName, token = result.nextToken))
        }
    }

    private fun serializeRecord(record: androidx.health.connect.client.records.Record): String {
        // Simplified serialization — production version should use proper per-type serialization
        return """{"id":"${record.metadata.id}","type":"${record::class.simpleName}","lastModified":"${record.metadata.lastModifiedTime}"}"""
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, HealthDispatchApp.SYNC_CHANNEL_ID)
            .setContentTitle(getString(R.string.sync_notification_title))
            .setContentText(getString(R.string.sync_notification_text))
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val TAG = "SyncForegroundService"
        const val NOTIFICATION_ID = 1001
        const val POLL_INTERVAL_MS = 15 * 60 * 1000L // 15 minutes
    }
}
