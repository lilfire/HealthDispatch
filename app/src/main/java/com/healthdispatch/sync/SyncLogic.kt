package com.healthdispatch.sync

import com.healthdispatch.data.cloud.HealthDispatchSupabaseClient
import com.healthdispatch.data.local.PendingSyncDao
import com.healthdispatch.data.local.SyncStatus
import javax.inject.Inject

class SyncLogic @Inject constructor(
    private val pendingSyncDao: PendingSyncDao,
    private val supabaseClient: HealthDispatchSupabaseClient
) {
    suspend fun syncPendingRecords(): Boolean {
        val pending = pendingSyncDao.getPendingRecords(limit = 50)
        if (pending.isEmpty()) return true

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

        return true
    }

    companion object {
        const val MAX_RETRIES = 5

        fun recordTypeToTable(type: String): String = when (type) {
            "Steps" -> "steps_records"
            "HeartRate" -> "heart_rate_records"
            "Sleep" -> "sleep_records"
            "Exercise" -> "exercise_records"
            "Weight" -> "body_records"
            else -> "health_records"
        }
    }
}
