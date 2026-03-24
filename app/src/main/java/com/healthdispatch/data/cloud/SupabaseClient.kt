package com.healthdispatch.data.cloud

import io.ktor.client.HttpClient
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseClient @Inject constructor(
    private val httpClient: HttpClient,
    private val config: CloudConfig
) {
    suspend fun pushRecords(tableName: String, jsonPayloads: List<String>): Result<Int> {
        return try {
            val body = "[${jsonPayloads.joinToString(",")}]"
            val response = httpClient.post("${config.url}/rest/v1/$tableName") {
                contentType(ContentType.Application.Json)
                headers {
                    append("apikey", config.apiKey)
                    append("Authorization", "Bearer ${config.apiKey}")
                    append("Prefer", "return=minimal")
                }
                setBody(body)
            }
            if (response.status.isSuccess()) {
                Result.success(jsonPayloads.size)
            } else {
                Result.failure(Exception("Supabase error ${response.status}: ${response.bodyAsText()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class CloudConfig(
    val url: String,
    val apiKey: String
)
