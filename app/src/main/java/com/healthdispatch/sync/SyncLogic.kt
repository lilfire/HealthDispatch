package com.healthdispatch.sync

import com.healthdispatch.data.cloud.FirestoreHealthRepository
import com.healthdispatch.data.local.PendingSyncDao
import com.healthdispatch.data.local.SyncStatus
import javax.inject.Inject

class SyncLogic @Inject constructor(
    private val pendingSyncDao: PendingSyncDao,
    private val firestoreRepo: FirestoreHealthRepository
) {
    suspend fun syncPendingRecords(): Boolean {
        val pending = pendingSyncDao.getPendingRecords(limit = 50)
        if (pending.isEmpty()) return true

        val grouped = pending.groupBy { it.recordType }

        for ((recordType, records) in grouped) {
            val collection = recordTypeToCollection(recordType)
            val payloads = records.map { it.jsonPayload }

            records.forEach {
                pendingSyncDao.updateStatus(it.id, SyncStatus.IN_PROGRESS)
            }

            val result = firestoreRepo.pushRecords(collection, payloads)
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

        fun recordTypeToCollection(type: String): String = when (type) {
            "Steps" -> "steps"
            "HeartRate" -> "heart_rate"
            "Sleep" -> "sleep"
            "Exercise" -> "exercise"
            "Weight" -> "body"
            else -> "health_records"
        }
    }
}
