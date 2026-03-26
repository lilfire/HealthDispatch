package com.healthdispatch.data.cloud

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthDispatchSupabaseClient @Inject constructor(
    private val postgrestClient: PostgrestClientWrapper,
    private val authProvider: AuthSessionProvider
) {
    suspend fun pushRecords(tableName: String, jsonPayloads: List<String>): Result<Int> {
        if (jsonPayloads.isEmpty()) return Result.success(0)

        val userId = authProvider.currentUserId()
            ?: return Result.failure(IllegalStateException("Not authenticated"))

        return try {
            val withUserId = buildJsonArray {
                for (payload in jsonPayloads) {
                    val original = Json.parseToJsonElement(payload).jsonObject
                    add(buildJsonObject {
                        for ((key, value) in original) {
                            put(key, value)
                        }
                        put("user_id", JsonPrimitive(userId))
                    })
                }
            }
            postgrestClient.insert(tableName, withUserId.toString())
            Result.success(jsonPayloads.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
