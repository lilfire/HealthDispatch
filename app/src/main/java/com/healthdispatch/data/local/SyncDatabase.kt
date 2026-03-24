package com.healthdispatch.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PendingSyncRecord::class, SyncToken::class],
    version = 1,
    exportSchema = false
)
abstract class SyncDatabase : RoomDatabase() {
    abstract fun pendingSyncDao(): PendingSyncDao
    abstract fun syncTokenDao(): SyncTokenDao
}
