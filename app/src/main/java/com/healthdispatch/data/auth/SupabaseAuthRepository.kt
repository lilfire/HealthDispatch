package com.healthdispatch.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.ktor.client.HttpClient
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class AuthTokenResponse(
    val access_token: String,
    val refresh_token: String = "",
    val token_type: String = "bearer",
    val expires_in: Long = 3600
)

@Serializable
data class AuthErrorResponse(
    val error: String = "",
    val error_description: String = "",
    val msg: String = ""
)

@Serializable
data class AuthCredentialsRequest(
    val email: String,
    val password: String
)

@Serializable
data class AuthOAuthRequest(
    val provider: String,
    val id_token: String,
    val nonce: String = ""
)

@Singleton
class SupabaseAuthRepository @Inject constructor(
    private val httpClient: HttpClient,
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
    private val supabaseUrl: String = "",
    private val supabaseApiKey: String = ""
) : AuthRepository {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unknown)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    override suspend fun refreshAuthState() {
        val token = dataStore.data.map { prefs ->
            prefs[ACCESS_TOKEN_KEY]
        }.first()
        _authState.value = if (token.isNullOrBlank()) {
            AuthState.Unauthenticated
        } else {
            AuthState.Authenticated
        }
    }

    override suspend fun signIn(email: String, password: String): Result<Unit> {
        return performAuth(
            endpoint = "/auth/v1/token?grant_type=password",
            body = json.encodeToString(AuthCredentialsRequest.serializer(), AuthCredentialsRequest(email, password))
        )
    }

    override suspend fun signUp(email: String, password: String): Result<Unit> {
        return performAuth(
            endpoint = "/auth/v1/signup",
            body = json.encodeToString(AuthCredentialsRequest.serializer(), AuthCredentialsRequest(email, password))
        )
    }

    override suspend fun signInWithGoogle(idToken: String, nonce: String): Result<Unit> {
        return performAuth(
            endpoint = "/auth/v1/token?grant_type=id_token",
            body = json.encodeToString(AuthOAuthRequest.serializer(), AuthOAuthRequest(provider = "google", id_token = idToken, nonce = nonce))
        )
    }

    override suspend fun signInWithApple(idToken: String): Result<Unit> {
        return performAuth(
            endpoint = "/auth/v1/token?grant_type=id_token",
            body = json.encodeToString(AuthOAuthRequest.serializer(), AuthOAuthRequest(provider = "apple", id_token = idToken))
        )
    }

    override suspend fun signInWithFacebook(accessToken: String): Result<Unit> {
        return performAuth(
            endpoint = "/auth/v1/token?grant_type=id_token",
            body = json.encodeToString(AuthOAuthRequest.serializer(), AuthOAuthRequest(provider = "facebook", id_token = accessToken))
        )
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            val token = dataStore.data.map { it[ACCESS_TOKEN_KEY] }.first()
            if (!token.isNullOrBlank()) {
                val (url, apiKey) = resolveConfig()
                httpClient.post("$url/auth/v1/logout") {
                    contentType(ContentType.Application.Json)
                    headers {
                        append("apikey", apiKey)
                        append("Authorization", "Bearer $token")
                    }
                }
            }
            dataStore.edit { prefs ->
                prefs.remove(ACCESS_TOKEN_KEY)
                prefs.remove(REFRESH_TOKEN_KEY)
            }
            _authState.value = AuthState.Unauthenticated
            Result.success(Unit)
        } catch (e: Exception) {
            // Clear local tokens even if network call fails
            dataStore.edit { prefs ->
                prefs.remove(ACCESS_TOKEN_KEY)
                prefs.remove(REFRESH_TOKEN_KEY)
            }
            _authState.value = AuthState.Unauthenticated
            Result.success(Unit)
        }
    }

    private suspend fun resolveConfig(): Pair<String, String> {
        val dsUrl = dataStore.data.map { it[SUPABASE_URL_KEY] }.first() ?: ""
        val dsKey = dataStore.data.map { it[SUPABASE_API_KEY] }.first() ?: ""
        val url = dsUrl.ifBlank { supabaseUrl }
        val apiKey = dsKey.ifBlank { supabaseApiKey }
        return url to apiKey
    }

    private suspend fun performAuth(endpoint: String, body: String): Result<Unit> {
        return try {
            val (url, apiKey) = resolveConfig()

            if (url.isBlank() || apiKey.isBlank()
                || url == "https://your-project.supabase.co"
                || apiKey == "your-anon-key") {
                return Result.failure(Exception("Supabase is not configured. Please set SUPABASE_URL and SUPABASE_ANON_KEY in local.properties or environment variables."))
            }

            val response = httpClient.post("$url$endpoint") {
                contentType(ContentType.Application.Json)
                headers {
                    append("apikey", apiKey)
                }
                setBody(body)
            }

            if (response.status.isSuccess()) {
                val responseBody = response.bodyAsText()
                val tokenResponse = json.decodeFromString<AuthTokenResponse>(responseBody)
                dataStore.edit { prefs ->
                    prefs[ACCESS_TOKEN_KEY] = tokenResponse.access_token
                    prefs[REFRESH_TOKEN_KEY] = tokenResponse.refresh_token
                }
                _authState.value = AuthState.Authenticated
                Result.success(Unit)
            } else if (response.status == HttpStatusCode.TooManyRequests) {
                Result.failure(Exception("Too many attempts. Please wait a moment and try again"))
            } else {
                val errorBody = response.bodyAsText()
                val rawMsg = try {
                    val error = json.decodeFromString<AuthErrorResponse>(errorBody)
                    error.error_description.ifBlank { error.msg.ifBlank { error.error } }
                } catch (_: Exception) {
                    "Authentication failed (${response.status})"
                }
                Result.failure(Exception(mapErrorMessage(rawMsg)))
            }
        } catch (e: java.net.UnknownHostException) {
            Result.failure(Exception("No internet connection. Please check your network and try again"))
        } catch (e: java.net.ConnectException) {
            Result.failure(Exception("No internet connection. Please check your network and try again"))
        } catch (e: java.net.SocketTimeoutException) {
            Result.failure(Exception("Connection timed out. Please check your network and try again"))
        } catch (e: java.io.IOException) {
            Result.failure(Exception("No internet connection. Please check your network and try again"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapErrorMessage(raw: String): String {
        val lower = raw.lowercase()
        return when {
            lower.contains("invalid login credentials") ->
                "The email or password you entered is incorrect"
            lower.contains("email not confirmed") ->
                "Please verify your email address before signing in"
            lower.contains("user already registered") ->
                "An account with this email already exists. Try signing in instead"
            lower.contains("password should be at least") || lower.contains("weak_password") ->
                "Password must be at least 6 characters long"
            lower.contains("request_rate_limit") || lower.contains("rate limit") ->
                "Too many attempts. Please wait a moment and try again"
            else -> raw
        }
    }

    companion object {
        val ACCESS_TOKEN_KEY = stringPreferencesKey("auth_access_token")
        val REFRESH_TOKEN_KEY = stringPreferencesKey("auth_refresh_token")
        val SUPABASE_URL_KEY = stringPreferencesKey("supabase_url")
        val SUPABASE_API_KEY = stringPreferencesKey("supabase_api_key")
    }
}
