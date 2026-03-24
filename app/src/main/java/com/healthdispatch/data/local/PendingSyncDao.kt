package com.healthdispatch.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingSyncDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: PendingSyncRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<PendingSyncRecord>)

    @Query("SELECT * FROM pending_sync_records WHERE status = 'PENDING' OR status = 'FAILED' ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getPendingRecords(limit: Int = 100): List<PendingSyncRecord>

    @Query("UPDATE pending_sync_records SET status = :status, lastAttemptAt = :timestamp, retryCount = retryCount + 1, errorMessage = :error WHERE id = :id")
    suspend fun updateStatus(id: Long, status: SyncStatus, timestamp: Long = System.currentTimeMillis(), error: String? = null)

    @Query("DELETE FROM pending_sync_records WHERE status = 'SYNCED'")
    suspend fun deleteSynced()

    @Query("SELECT COUNT(*) FROM pending_sync_records WHERE status = 'PENDING' OR status = 'FAILED'")
    suspend fun getPendingCount(): Int

    @Query("SELECT COUNT(*) FROM pending_sync_records WHERE status = 'PENDING' OR status = 'FAILED'")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM pending_sync_records WHERE status = 'SYNCED'")
    suspend fun getSyncedCount(): Int
}
