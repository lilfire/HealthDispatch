package com.healthdispatch.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_tokens")
data class SyncToken(
    @PrimaryKey
    val recordType: String,
    val token: String,
    val updatedAt: Long = System.currentTimeMillis()
)
