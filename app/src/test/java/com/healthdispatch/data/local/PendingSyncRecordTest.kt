package com.healthdispatch.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PendingSyncRecordTest {

    @Test
    fun pendingSyncRecord_defaultValues() {
        val record = PendingSyncRecord(
            recordType = "Steps",
            recordId = "step-1",
            jsonPayload = "{\"count\": 100}"
        )
        assertEquals(0L, record.id)
        assertEquals(SyncStatus.PENDING, record.status)
        assertEquals(0, record.retryCount)
        assertNull(record.lastAttemptAt)
        assertNull(record.errorMessage)
    }

    @Test
    fun pendingSyncRecord_customValues() {
        val record = PendingSyncRecord(
            id = 42,
            recordType = "HeartRate",
            recordId = "hr-1",
            jsonPayload = "{\"bpm\": 72}",
            status = SyncStatus.IN_PROGRESS,
            retryCount = 3,
            createdAt = 1000L,
            lastAttemptAt = 2000L,
            errorMessage = "Network error"
        )
        assertEquals(42L, record.id)
        assertEquals("HeartRate", record.recordType)
        assertEquals("hr-1", record.recordId)
        assertEquals("{\"bpm\": 72}", record.jsonPayload)
        assertEquals(SyncStatus.IN_PROGRESS, record.status)
        assertEquals(3, record.retryCount)
        assertEquals(1000L, record.createdAt)
        assertEquals(2000L, record.lastAttemptAt)
        assertEquals("Network error", record.errorMessage)
    }

    @Test
    fun syncStatus_allValuesExist() {
        val statuses = SyncStatus.entries
        assertEquals(4, statuses.size)
        assertEquals(SyncStatus.PENDING, statuses[0])
        assertEquals(SyncStatus.IN_PROGRESS, statuses[1])
        assertEquals(SyncStatus.SYNCED, statuses[2])
        assertEquals(SyncStatus.FAILED, statuses[3])
    }

    @Test
    fun syncStatus_valueOfFromString() {
        assertEquals(SyncStatus.PENDING, SyncStatus.valueOf("PENDING"))
        assertEquals(SyncStatus.IN_PROGRESS, SyncStatus.valueOf("IN_PROGRESS"))
        assertEquals(SyncStatus.SYNCED, SyncStatus.valueOf("SYNCED"))
        assertEquals(SyncStatus.FAILED, SyncStatus.valueOf("FAILED"))
    }

    @Test
    fun syncToken_storesValues() {
        val token = SyncToken(
            recordType = "Steps",
            token = "abc123",
            updatedAt = 5000L
        )
        assertEquals("Steps", token.recordType)
        assertEquals("abc123", token.token)
        assertEquals(5000L, token.updatedAt)
    }

    @Test
    fun syncToken_equalityForSameValues() {
        val token1 = SyncToken(recordType = "Steps", token = "abc", updatedAt = 1000L)
        val token2 = SyncToken(recordType = "Steps", token = "abc", updatedAt = 1000L)
        assertEquals(token1, token2)
    }
}
