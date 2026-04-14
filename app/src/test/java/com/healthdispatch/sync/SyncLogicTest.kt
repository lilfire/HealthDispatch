package com.healthdispatch.sync

import com.healthdispatch.data.cloud.FirestoreHealthRepository
import com.healthdispatch.data.local.PendingSyncDao
import com.healthdispatch.data.local.PendingSyncRecord
import com.healthdispatch.data.local.SyncStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SyncLogicTest {

    private lateinit var pendingSyncDao: PendingSyncDao
    private lateinit var firestoreRepo: FirestoreHealthRepository
    private lateinit var syncLogic: SyncLogic

    @Before
    fun setup() {
        pendingSyncDao = mockk(relaxed = true)
        firestoreRepo = mockk(relaxed = true)
        syncLogic = SyncLogic(pendingSyncDao, firestoreRepo)
    }

    @Test
    fun `syncPendingRecords returns true when no pending records`() = runTest {
        coEvery { pendingSyncDao.getPendingRecords(limit = 50) } returns emptyList()

        val result = syncLogic.syncPendingRecords()

        assertTrue(result)
    }

    @Test
    fun `syncPendingRecords groups records by type and pushes to Firestore`() = runTest {
        val records = listOf(
            PendingSyncRecord(id = 1, recordType = "Steps", recordId = "s1", jsonPayload = """{"count":100}"""),
            PendingSyncRecord(id = 2, recordType = "Steps", recordId = "s2", jsonPayload = """{"count":200}"""),
            PendingSyncRecord(id = 3, recordType = "HeartRate", recordId = "h1", jsonPayload = """{"bpm":72}""")
        )
        coEvery { pendingSyncDao.getPendingRecords(limit = 50) } returns records
        coEvery { firestoreRepo.pushRecords("steps", any()) } returns Result.success(2)
        coEvery { firestoreRepo.pushRecords("heart_rate", any()) } returns Result.success(1)

        syncLogic.syncPendingRecords()

        coVerify { firestoreRepo.pushRecords("steps", listOf("""{"count":100}""", """{"count":200}""")) }
        coVerify { firestoreRepo.pushRecords("heart_rate", listOf("""{"bpm":72}""")) }
    }

    @Test
    fun `syncPendingRecords marks records as SYNCED on success`() = runTest {
        val records = listOf(
            PendingSyncRecord(id = 1, recordType = "Steps", recordId = "s1", jsonPayload = """{"count":100}""")
        )
        coEvery { pendingSyncDao.getPendingRecords(limit = 50) } returns records
        coEvery { firestoreRepo.pushRecords("steps", any()) } returns Result.success(1)

        syncLogic.syncPendingRecords()

        coVerify { pendingSyncDao.updateStatus(1, SyncStatus.SYNCED, any(), null) }
    }

    @Test
    fun `syncPendingRecords marks records as FAILED after max retries`() = runTest {
        val records = listOf(
            PendingSyncRecord(id = 1, recordType = "Steps", recordId = "s1", jsonPayload = """{"count":100}""", retryCount = 5)
        )
        coEvery { pendingSyncDao.getPendingRecords(limit = 50) } returns records
        coEvery { firestoreRepo.pushRecords("steps", any()) } returns Result.failure(RuntimeException("fail"))

        syncLogic.syncPendingRecords()

        coVerify { pendingSyncDao.updateStatus(1, SyncStatus.FAILED, any(), "fail") }
    }

    @Test
    fun `syncPendingRecords resets to PENDING when retries not exhausted`() = runTest {
        val records = listOf(
            PendingSyncRecord(id = 1, recordType = "Steps", recordId = "s1", jsonPayload = """{"count":100}""", retryCount = 2)
        )
        coEvery { pendingSyncDao.getPendingRecords(limit = 50) } returns records
        coEvery { firestoreRepo.pushRecords("steps", any()) } returns Result.failure(RuntimeException("fail"))

        syncLogic.syncPendingRecords()

        coVerify { pendingSyncDao.updateStatus(1, SyncStatus.PENDING, any(), "fail") }
    }

    @Test
    fun `recordTypeToCollection maps all known types correctly`() {
        assertEquals("steps", SyncLogic.recordTypeToCollection("Steps"))
        assertEquals("heart_rate", SyncLogic.recordTypeToCollection("HeartRate"))
        assertEquals("sleep", SyncLogic.recordTypeToCollection("Sleep"))
        assertEquals("exercise", SyncLogic.recordTypeToCollection("Exercise"))
        assertEquals("body", SyncLogic.recordTypeToCollection("Weight"))
        assertEquals("health_records", SyncLogic.recordTypeToCollection("Unknown"))
    }

    private fun assertEquals(expected: String, actual: String) {
        org.junit.Assert.assertEquals(expected, actual)
    }
}
