package com.healthdispatch.sync

import com.healthdispatch.data.cloud.HealthDispatchSupabaseClient
import com.healthdispatch.data.local.PendingSyncDao
import com.healthdispatch.data.local.PendingSyncRecord
import com.healthdispatch.data.local.SyncStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SyncWorkerTest {

    private lateinit var mockDao: PendingSyncDao
    private lateinit var mockClient: HealthDispatchSupabaseClient
    private lateinit var syncLogic: SyncLogic

    @Before
    fun setUp() {
        mockDao = mockk(relaxed = true)
        mockClient = mockk()
        syncLogic = SyncLogic(mockDao, mockClient)
    }

    @Test
    fun `sync returns success when no pending records`() = runTest {
        coEvery { mockDao.getPendingRecords(any()) } returns emptyList()

        val result = syncLogic.syncPendingRecords()

        assertEquals(true, result)
    }

    @Test
    fun `sync pushes records grouped by type`() = runTest {
        val records = listOf(
            makePendingRecord(1, "Steps", """{"id":"s1"}"""),
            makePendingRecord(2, "Steps", """{"id":"s2"}"""),
            makePendingRecord(3, "HeartRate", """{"id":"h1"}""")
        )
        coEvery { mockDao.getPendingRecords(any()) } returns records
        coEvery { mockClient.pushRecords(any(), any()) } returns Result.success(2)

        syncLogic.syncPendingRecords()

        coVerify { mockClient.pushRecords("steps_records", listOf("""{"id":"s1"}""", """{"id":"s2"}""")) }
        coVerify { mockClient.pushRecords("heart_rate_records", listOf("""{"id":"h1"}""")) }
    }

    @Test
    fun `sync marks records SYNCED on success`() = runTest {
        val records = listOf(makePendingRecord(1, "Steps", """{"id":"s1"}"""))
        coEvery { mockDao.getPendingRecords(any()) } returns records
        coEvery { mockClient.pushRecords(any(), any()) } returns Result.success(1)

        syncLogic.syncPendingRecords()

        coVerify { mockDao.updateStatus(1L, SyncStatus.SYNCED) }
    }

    @Test
    fun `sync marks records FAILED after max retries`() = runTest {
        val records = listOf(makePendingRecord(1, "Steps", """{"id":"s1"}""", retryCount = 5))
        coEvery { mockDao.getPendingRecords(any()) } returns records
        coEvery { mockClient.pushRecords(any(), any()) } returns Result.failure(Exception("fail"))

        syncLogic.syncPendingRecords()

        coVerify { mockDao.updateStatus(1L, SyncStatus.FAILED, any(), "fail") }
    }

    @Test
    fun `sync marks records PENDING when retries remaining`() = runTest {
        val records = listOf(makePendingRecord(1, "Steps", """{"id":"s1"}""", retryCount = 2))
        coEvery { mockDao.getPendingRecords(any()) } returns records
        coEvery { mockClient.pushRecords(any(), any()) } returns Result.failure(Exception("temp"))

        syncLogic.syncPendingRecords()

        coVerify { mockDao.updateStatus(1L, SyncStatus.PENDING, any(), "temp") }
    }

    @Test
    fun `recordTypeToTable maps all known types correctly`() {
        assertEquals("steps_records", SyncLogic.recordTypeToTable("Steps"))
        assertEquals("heart_rate_records", SyncLogic.recordTypeToTable("HeartRate"))
        assertEquals("sleep_records", SyncLogic.recordTypeToTable("Sleep"))
        assertEquals("exercise_records", SyncLogic.recordTypeToTable("Exercise"))
        assertEquals("body_records", SyncLogic.recordTypeToTable("Weight"))
        assertEquals("health_records", SyncLogic.recordTypeToTable("Unknown"))
    }

    private fun makePendingRecord(
        id: Long, type: String, payload: String, retryCount: Int = 0
    ) = PendingSyncRecord(
        id = id,
        recordType = type,
        recordId = "rid-$id",
        jsonPayload = payload,
        retryCount = retryCount
    )
}
