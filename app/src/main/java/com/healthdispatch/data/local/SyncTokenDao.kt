package com.healthdispatch.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SyncTokenDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(token: SyncToken)

    @Query("SELECT * FROM sync_tokens WHERE recordType = :recordType")
    suspend fun getToken(recordType: String): SyncToken?

    @Query("SELECT * FROM sync_tokens")
    suspend fun getAllTokens(): List<SyncToken>
}
