package com.healthdispatch.data.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.changes.Change
import androidx.health.connect.client.changes.UpsertionChange
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass

@Singleton
class HealthConnectRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val client: HealthConnectClient by lazy {
        HealthConnectClient.getOrCreate(context)
    }

    val requiredPermissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
    )

    val supportedRecordTypes: List<KClass<out Record>> = listOf(
        StepsRecord::class,
        HeartRateRecord::class,
        SleepSessionRecord::class,
        ExerciseSessionRecord::class,
        WeightRecord::class,
    )

    suspend fun hasAllPermissions(): Boolean {
        val granted = client.permissionController.getGrantedPermissions()
        return requiredPermissions.all { it in granted }
    }

    suspend fun isAvailable(): Boolean {
        return HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
    }

    suspend fun getChangesToken(recordTypes: Set<KClass<out Record>>): String {
        return client.getChangesToken(
            ChangesTokenRequest(recordTypes = recordTypes)
        )
    }

    suspend fun getChanges(token: String): ChangesResult {
        var currentToken = token
        val allChanges = mutableListOf<Change>()

        do {
            val response = client.getChanges(currentToken)
            allChanges.addAll(response.changes)
            currentToken = response.nextChangesToken
        } while (response.hasMore)

        return ChangesResult(
            changes = allChanges,
            nextToken = currentToken
        )
    }

    suspend fun <T : Record> readRecords(
        recordType: KClass<T>,
        startTime: Instant,
        endTime: Instant = Instant.now()
    ): List<T> {
        val request = ReadRecordsRequest(
            recordType = recordType,
            timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
        )
        return client.readRecords(request).records
    }

    data class ChangesResult(
        val changes: List<Change>,
        val nextToken: String
    )
}
