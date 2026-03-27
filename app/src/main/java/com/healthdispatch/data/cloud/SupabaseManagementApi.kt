package com.healthdispatch.data.cloud

import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class SupabaseProject(
    val id: String,
    val name: String,
    @SerialName("organization_id") val organizationId: String,
    val region: String,
    @SerialName("created_at") val createdAt: String = "",
    val status: String = ""
)

@Serializable
data class SupabaseApiKey(
    val name: String,
    @SerialName("api_key") val apiKey: String
)

@Serializable
data class SupabaseOrganization(
    val id: String,
    val name: String
)

@Serializable
private data class CreateProjectRequest(
    val name: String,
    @SerialName("organization_id") val organizationId: String,
    @SerialName("db_pass") val dbPass: String,
    val region: String,
    @SerialName("plan") val plan: String = "free"
)

@Serializable
private data class RunSqlRequest(
    val query: String
)

interface SupabaseManagementApi {
    suspend fun listProjects(accessToken: String): Result<List<SupabaseProject>>
    suspend fun createProject(
        accessToken: String,
        name: String,
        organizationId: String,
        dbPassword: String,
        region: String
    ): Result<SupabaseProject>
    suspend fun getApiKeys(accessToken: String, projectRef: String): Result<List<SupabaseApiKey>>
    suspend fun runSql(accessToken: String, projectRef: String, sql: String): Result<Unit>
    suspend fun getOrganizations(accessToken: String): Result<List<SupabaseOrganization>>
}

@Singleton
class SupabaseManagementApiImpl @Inject constructor(
    private val httpClient: HttpClient,
    private val json: Json
) : SupabaseManagementApi {

    private companion object {
        const val BASE_URL = "https://api.supabase.com/v1"
    }

    override suspend fun listProjects(accessToken: String): Result<List<SupabaseProject>> {
        return try {
            val response = httpClient.get("$BASE_URL/projects") {
                bearerAuth(accessToken)
            }
            if (response.status.isSuccess()) {
                val body = response.bodyAsText()
                Result.success(json.decodeFromString<List<SupabaseProject>>(body))
            } else {
                Result.failure(Exception("Failed to list projects: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createProject(
        accessToken: String,
        name: String,
        organizationId: String,
        dbPassword: String,
        region: String
    ): Result<SupabaseProject> {
        return try {
            val response = httpClient.post("$BASE_URL/projects") {
                bearerAuth(accessToken)
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(
                    CreateProjectRequest.serializer(),
                    CreateProjectRequest(
                        name = name,
                        organizationId = organizationId,
                        dbPass = dbPassword,
                        region = region
                    )
                ))
            }
            if (response.status.isSuccess()) {
                val body = response.bodyAsText()
                Result.success(json.decodeFromString<SupabaseProject>(body))
            } else {
                val errorBody = response.bodyAsText()
                Result.failure(Exception("Failed to create project: $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getApiKeys(accessToken: String, projectRef: String): Result<List<SupabaseApiKey>> {
        return try {
            val response = httpClient.get("$BASE_URL/projects/$projectRef/api-keys") {
                bearerAuth(accessToken)
            }
            if (response.status.isSuccess()) {
                val body = response.bodyAsText()
                Result.success(json.decodeFromString<List<SupabaseApiKey>>(body))
            } else {
                Result.failure(Exception("Failed to get API keys: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun runSql(accessToken: String, projectRef: String, sql: String): Result<Unit> {
        return try {
            val response = httpClient.post("$BASE_URL/projects/$projectRef/database/query") {
                bearerAuth(accessToken)
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(RunSqlRequest.serializer(), RunSqlRequest(query = sql)))
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                val errorBody = response.bodyAsText()
                Result.failure(Exception("Failed to run SQL: $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getOrganizations(accessToken: String): Result<List<SupabaseOrganization>> {
        return try {
            val response = httpClient.get("$BASE_URL/organizations") {
                bearerAuth(accessToken)
            }
            if (response.status.isSuccess()) {
                val body = response.bodyAsText()
                Result.success(json.decodeFromString<List<SupabaseOrganization>>(body))
            } else {
                Result.failure(Exception("Failed to get organizations: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
