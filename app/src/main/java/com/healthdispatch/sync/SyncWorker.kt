package com.healthdispatch.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncLogic: SyncLogic
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            syncLogic.syncPendingRecords()
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            Result.retry()
        }
    }

    companion object {
        const val TAG = "SyncWorker"
        const val WORK_NAME = "health_sync_periodic"
    }
}
