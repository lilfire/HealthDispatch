package com.healthdispatch.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.healthdispatch.data.cloud.SupabaseClient
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SyncWorkerTest {

    private lateinit var pendingSyncDao: PendingSyncDao
    private lateinit var supabaseClient: SupabaseClient
    private lateinit var worker: SyncWorker

    @Before
    fun setup() {
        pendingSyncDao = mockk(relaxed = true)
        supabaseClient = mockk(relaxed = true)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val params = mockk<WorkerParameters>(relaxed = true)
        worker = SyncWorker(context, params, pendingSyncDao, supabaseClient)
    }

    @Test
    fun doWork_noPendingRecords_returnsSuccess() = runTest {
        coEvery { pendingSyncDao.getPendingRecords(any()) } returns emptyList()

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun doWork_recordsGroupedByType() = runTest {
        val records = listOf(
            createRecord(1, "Steps", "{\"count\":100}"),
            createRecord(2, "Steps", "{\"count\":200}"),
            createRecord(3, "HeartRate", "{\"bpm\":72}")
        )
        coEvery { pendingSyncDao.getPendingRecords(any()) } returns records
        coEvery { supabaseClient.pushRecords(any(), any()) } returns Result.success(1)

        worker.doWork()

        coVerify { supabaseClient.pushRecords("steps_records", listOf("{\"count\":100}", "{\"count\":200}")) }
        coVerify { supabaseClient.pushRecords("heart_rate_records", listOf("{\"bpm\":72}")) }
    }

    @Test
    fun doWork_successfulPush_updatesStatusToSynced() = runTest {
        val records = listOf(createRecord(1, "Steps", "{\"count\":100}"))
        coEvery { pendingSyncDao.getPendingRecords(any()) } returns records
        coEvery { supabaseClient.pushRecords(any(), any()) } returns Result.success(1)

        worker.doWork()

        coVerify { pendingSyncDao.updateStatus(1, SyncStatus.SYNCED) }
    }

    @Test
    fun doWork_failedPush_setsStatusToPendingWhenBelowMaxRetries() = runTest {
        val record = createRecord(1, "Steps", "{}", retryCount = 2)
        coEvery { pendingSyncDao.getPendingRecords(any()) } returns listOf(record)
        coEvery { supabaseClient.pushRecords(any(), any()) } returns Result.failure(Exception("Network error"))

        worker.doWork()

        coVerify {
            pendingSyncDao.updateStatus(1, SyncStatus.PENDING, any(), error = "Network error")
        }
    }

    @Test
    fun doWork_failedPush_setsStatusToFailedAtMaxRetries() = runTest {
        val record = createRecord(1, "Steps", "{}", retryCount = SyncWorker.MAX_RETRIES)
        coEvery { pendingSyncDao.getPendingRecords(any()) } returns listOf(record)
        coEvery { supabaseClient.pushRecords(any(), any()) } returns Result.failure(Exception("Network error"))

        worker.doWork()

        coVerify {
            pendingSyncDao.updateStatus(1, SyncStatus.FAILED, any(), error = "Network error")
        }
    }

    @Test
    fun doWork_failedPush_setsStatusToFailedAboveMaxRetries() = runTest {
        val record = createRecord(1, "Steps", "{}", retryCount = SyncWorker.MAX_RETRIES + 1)
        coEvery { pendingSyncDao.getPendingRecords(any()) } returns listOf(record)
        coEvery { supabaseClient.pushRecords(any(), any()) } returns Result.failure(Exception("error"))

        worker.doWork()

        coVerify {
            pendingSyncDao.updateStatus(1, SyncStatus.FAILED, any(), error = "error")
        }
    }

    @Test
    fun doWork_exception_returnsRetry() = runTest {
        coEvery { pendingSyncDao.getPendingRecords(any()) } throws RuntimeException("DB error")

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.retry(), result)
    }

    @Test
    fun doWork_setsInProgressBeforePush() = runTest {
        val records = listOf(createRecord(1, "Steps", "{\"count\":100}"))
        coEvery { pendingSyncDao.getPendingRecords(any()) } returns records
        coEvery { supabaseClient.pushRecords(any(), any()) } returns Result.success(1)

        worker.doWork()

        coVerify { pendingSyncDao.updateStatus(1, SyncStatus.IN_PROGRESS) }
    }

    @Test
    fun maxRetries_isFive() {
        assertEquals(5, SyncWorker.MAX_RETRIES)
    }

    @Test
    fun workName_isCorrect() {
        assertEquals("health_sync_periodic", SyncWorker.WORK_NAME)
    }

    @Test
    fun doWork_multipleRecordTypes_mapsToCorrectTables() = runTest {
        val records = listOf(
            createRecord(1, "Steps", "{}"),
            createRecord(2, "HeartRate", "{}"),
            createRecord(3, "Sleep", "{}"),
            createRecord(4, "Exercise", "{}"),
            createRecord(5, "Weight", "{}"),
            createRecord(6, "UnknownType", "{}")
        )
        coEvery { pendingSyncDao.getPendingRecords(any()) } returns records
        coEvery { supabaseClient.pushRecords(any(), any()) } returns Result.success(1)

        worker.doWork()

        coVerify { supabaseClient.pushRecords("steps_records", any()) }
        coVerify { supabaseClient.pushRecords("heart_rate_records", any()) }
        coVerify { supabaseClient.pushRecords("sleep_records", any()) }
        coVerify { supabaseClient.pushRecords("exercise_records", any()) }
        coVerify { supabaseClient.pushRecords("body_records", any()) }
        coVerify { supabaseClient.pushRecords("health_records", any()) }
    }

    private fun createRecord(
        id: Long,
        recordType: String,
        jsonPayload: String,
        retryCount: Int = 0
    ) = PendingSyncRecord(
        id = id,
        recordType = recordType,
        recordId = "$recordType-$id",
        jsonPayload = jsonPayload,
        status = SyncStatus.PENDING,
        retryCount = retryCount
    )
}
