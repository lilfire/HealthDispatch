package com.healthdispatch.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.healthdispatch.data.cloud.SupabaseClient
import com.healthdispatch.data.local.PendingSyncDao
import com.healthdispatch.data.local.SyncStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val pendingSyncDao: PendingSyncDao,
    private val supabaseClient: SupabaseClient
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val pending = pendingSyncDao.getPendingRecords(limit = 50)
            if (pending.isEmpty()) return Result.success()

            val grouped = pending.groupBy { it.recordType }

            for ((recordType, records) in grouped) {
                val tableName = recordTypeToTable(recordType)
                val payloads = records.map { it.jsonPayload }

                records.forEach {
                    pendingSyncDao.updateStatus(it.id, SyncStatus.IN_PROGRESS)
                }

                val result = supabaseClient.pushRecords(tableName, payloads)
                result.fold(
                    onSuccess = {
                        records.forEach {
                            pendingSyncDao.updateStatus(it.id, SyncStatus.SYNCED)
                        }
                    },
                    onFailure = { error ->
                        records.forEach {
                            pendingSyncDao.updateStatus(
                                it.id,
                                if (it.retryCount >= MAX_RETRIES) SyncStatus.FAILED else SyncStatus.PENDING,
                                error = error.message
                            )
                        }
                    }
                )
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            Result.retry()
        }
    }

    private fun recordTypeToTable(type: String): String = when (type) {
        "Steps" -> "steps_records"
        "HeartRate" -> "heart_rate_records"
        "Sleep" -> "sleep_records"
        "Exercise" -> "exercise_records"
        "Weight" -> "body_records"
        else -> "health_records"
    }

    companion object {
        const val TAG = "SyncWorker"
        const val MAX_RETRIES = 5
        const val WORK_NAME = "health_sync_periodic"
    }
}
