package com.healthdispatch.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_sync_records")
data class PendingSyncRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recordType: String,
    val recordId: String,
    val jsonPayload: String,
    val status: SyncStatus = SyncStatus.PENDING,
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastAttemptAt: Long? = null,
    val errorMessage: String? = null
)

enum class SyncStatus {
    PENDING,
    IN_PROGRESS,
    SYNCED,
    FAILED
}
