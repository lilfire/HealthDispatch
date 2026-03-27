package com.healthdispatch.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import app.cash.turbine.test
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class SupabaseAuthRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private lateinit var dataStore: DataStore<Preferences>

    @Before
    fun setup() {
        dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { File(tempFolder.newFolder(), "test_prefs.preferences_pb") }
        )
    }

    private fun createRepository(
        statusCode: HttpStatusCode = HttpStatusCode.OK,
        responseBody: String = """{"access_token":"test-token","refresh_token":"test-refresh","token_type":"bearer","expires_in":3600}"""
    ): SupabaseAuthRepository {
        val mockEngine = MockEngine { request ->
            respond(
                content = responseBody,
                status = statusCode,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(json) }
        }
        return SupabaseAuthRepository(httpClient, dataStore, json)
    }

    private suspend fun seedConfig() {
        dataStore.edit { prefs ->
            prefs[SupabaseAuthRepository.SUPABASE_URL_KEY] = "https://test.supabase.co"
            prefs[SupabaseAuthRepository.SUPABASE_API_KEY] = "test-api-key"
        }
    }

    @Test
    fun `initial auth state is Unknown`() = testScope.runTest {
        val repo = createRepository()
        assertEquals(AuthState.Unknown, repo.authState.value)
    }

    @Test
    fun `refreshAuthState sets Unauthenticated when no token`() = testScope.runTest {
        val repo = createRepository()
        repo.refreshAuthState()
        assertEquals(AuthState.Unauthenticated, repo.authState.value)
    }

    @Test
    fun `refreshAuthState sets Authenticated when token exists`() = testScope.runTest {
        dataStore.edit { it[SupabaseAuthRepository.ACCESS_TOKEN_KEY] = "existing-token" }
        val repo = createRepository()
        repo.refreshAuthState()
        assertEquals(AuthState.Authenticated, repo.authState.value)
    }

    @Test
    fun `signIn succeeds and stores tokens`() = testScope.runTest {
        seedConfig()
        val repo = createRepository()
        val result = repo.signIn("user@example.com", "password123")
        assertTrue(result.isSuccess)
        assertEquals(AuthState.Authenticated, repo.authState.value)
    }

    @Test
    fun `signIn fails with missing config`() = testScope.runTest {
        val repo = createRepository()
        val result = repo.signIn("user@example.com", "password123")
        assertTrue(result.isFailure)
        assertEquals("Supabase URL and API Key must be configured", result.exceptionOrNull()?.message)
    }

    @Test
    fun `signIn fails with invalid credentials maps to friendly message`() = testScope.runTest {
        seedConfig()
        val repo = createRepository(
            statusCode = HttpStatusCode.BadRequest,
            responseBody = """{"error":"invalid_grant","error_description":"Invalid login credentials"}"""
        )
        val result = repo.signIn("user@example.com", "wrongpassword")
        assertTrue(result.isFailure)
        assertEquals("The email or password you entered is incorrect", result.exceptionOrNull()?.message)
    }

    @Test
    fun `signIn fails with email not confirmed maps to friendly message`() = testScope.runTest {
        seedConfig()
        val repo = createRepository(
            statusCode = HttpStatusCode.BadRequest,
            responseBody = """{"error":"invalid_grant","error_description":"Email not confirmed"}"""
        )
        val result = repo.signIn("user@example.com", "password123")
        assertTrue(result.isFailure)
        assertEquals("Please verify your email address before signing in", result.exceptionOrNull()?.message)
    }

    @Test
    fun `signIn fails with rate limit maps to friendly message`() = testScope.runTest {
        seedConfig()
        val repo = createRepository(
            statusCode = HttpStatusCode.TooManyRequests,
            responseBody = """{"error":"over_request_rate_limit","error_description":"For security purposes, you can only request this after 60 seconds."}"""
        )
        val result = repo.signIn("user@example.com", "password123")
        assertTrue(result.isFailure)
        assertEquals("Too many attempts. Please wait a moment and try again", result.exceptionOrNull()?.message)
    }

    @Test
    fun `signUp succeeds`() = testScope.runTest {
        seedConfig()
        val repo = createRepository()
        val result = repo.signUp("new@example.com", "password123")
        assertTrue(result.isSuccess)
        assertEquals(AuthState.Authenticated, repo.authState.value)
    }

    @Test
    fun `signUp fails with duplicate email maps to friendly message`() = testScope.runTest {
        seedConfig()
        val repo = createRepository(
            statusCode = HttpStatusCode.UnprocessableEntity,
            responseBody = """{"error":"","error_description":"User already registered","msg":""}"""
        )
        val result = repo.signUp("existing@example.com", "password123")
        assertTrue(result.isFailure)
        assertEquals("An account with this email already exists. Try signing in instead", result.exceptionOrNull()?.message)
    }

    @Test
    fun `signUp fails with weak password maps to friendly message`() = testScope.runTest {
        seedConfig()
        val repo = createRepository(
            statusCode = HttpStatusCode.UnprocessableEntity,
            responseBody = """{"error":"weak_password","error_description":"Password should be at least 6 characters","msg":""}"""
        )
        val result = repo.signUp("new@example.com", "12345")
        assertTrue(result.isFailure)
        assertEquals("Password must be at least 6 characters long", result.exceptionOrNull()?.message)
    }

    @Test
    fun `signInWithGoogle succeeds`() = testScope.runTest {
        seedConfig()
        val repo = createRepository()
        val result = repo.signInWithGoogle("google-id-token")
        assertTrue(result.isSuccess)
        assertEquals(AuthState.Authenticated, repo.authState.value)
    }

    @Test
    fun `signOut clears tokens and sets Unauthenticated`() = testScope.runTest {
        seedConfig()
        dataStore.edit {
            it[SupabaseAuthRepository.ACCESS_TOKEN_KEY] = "existing-token"
            it[SupabaseAuthRepository.REFRESH_TOKEN_KEY] = "existing-refresh"
        }
        val repo = createRepository()
        val result = repo.signOut()
        assertTrue(result.isSuccess)
        assertEquals(AuthState.Unauthenticated, repo.authState.value)
    }

    @Test
    fun `signOut succeeds even on network error`() = testScope.runTest {
        // Create a repo with a failing engine
        val failingEngine = MockEngine { _ ->
            throw Exception("Network error")
        }
        val httpClient = HttpClient(failingEngine) {
            install(ContentNegotiation) { json(json) }
        }
        val repo = SupabaseAuthRepository(httpClient, dataStore, json)

        dataStore.edit {
            it[SupabaseAuthRepository.SUPABASE_URL_KEY] = "https://test.supabase.co"
            it[SupabaseAuthRepository.SUPABASE_API_KEY] = "test-api-key"
            it[SupabaseAuthRepository.ACCESS_TOKEN_KEY] = "token"
        }

        val result = repo.signOut()
        assertTrue(result.isSuccess)
        assertEquals(AuthState.Unauthenticated, repo.authState.value)
    }

    @Test
    fun `signIn fails with network error maps to friendly message`() = testScope.runTest {
        seedConfig()
        val failingEngine = MockEngine { _ ->
            throw java.net.UnknownHostException("Unable to resolve host")
        }
        val httpClient = HttpClient(failingEngine) {
            install(ContentNegotiation) { json(json) }
        }
        val repo = SupabaseAuthRepository(httpClient, dataStore, json)
        val result = repo.signIn("user@example.com", "password123")
        assertTrue(result.isFailure)
        assertEquals("No internet connection. Please check your network and try again", result.exceptionOrNull()?.message)
    }

    @Test
    fun `signIn fails with connect exception maps to friendly message`() = testScope.runTest {
        seedConfig()
        val failingEngine = MockEngine { _ ->
            throw java.net.ConnectException("Connection refused")
        }
        val httpClient = HttpClient(failingEngine) {
            install(ContentNegotiation) { json(json) }
        }
        val repo = SupabaseAuthRepository(httpClient, dataStore, json)
        val result = repo.signIn("user@example.com", "password123")
        assertTrue(result.isFailure)
        assertEquals("No internet connection. Please check your network and try again", result.exceptionOrNull()?.message)
    }

    @Test
    fun `signIn fails with socket timeout maps to friendly message`() = testScope.runTest {
        seedConfig()
        val failingEngine = MockEngine { _ ->
            throw java.net.SocketTimeoutException("Read timed out")
        }
        val httpClient = HttpClient(failingEngine) {
            install(ContentNegotiation) { json(json) }
        }
        val repo = SupabaseAuthRepository(httpClient, dataStore, json)
        val result = repo.signIn("user@example.com", "password123")
        assertTrue(result.isFailure)
        assertEquals("Connection timed out. Please check your network and try again", result.exceptionOrNull()?.message)
    }

    @Test
    fun `signIn fails with io exception maps to friendly message`() = testScope.runTest {
        seedConfig()
        val failingEngine = MockEngine { _ ->
            throw java.io.IOException("Stream reset")
        }
        val httpClient = HttpClient(failingEngine) {
            install(ContentNegotiation) { json(json) }
        }
        val repo = SupabaseAuthRepository(httpClient, dataStore, json)
        val result = repo.signIn("user@example.com", "password123")
        assertTrue(result.isFailure)
        assertEquals("No internet connection. Please check your network and try again", result.exceptionOrNull()?.message)
    }

    private fun createRepositoryCapturingBody(
        capturedBody: MutableList<String>,
        statusCode: HttpStatusCode = HttpStatusCode.OK,
        responseBody: String = """{"access_token":"test-token","refresh_token":"test-refresh","token_type":"bearer","expires_in":3600}"""
    ): SupabaseAuthRepository {
        val mockEngine = MockEngine { request ->
            capturedBody.add(request.body.toByteArray().decodeToString())
            respond(
                content = responseBody,
                status = statusCode,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(json) }
        }
        return SupabaseAuthRepository(httpClient, dataStore, json)
    }

    @Test
    fun `signIn with password containing double quotes produces valid JSON`() = testScope.runTest {
        seedConfig()
        val capturedBody = mutableListOf<String>()
        val repo = createRepositoryCapturingBody(capturedBody)
        val result = repo.signIn("user@example.com", """pass"word""")
        assertTrue(result.isSuccess)
        val parsed = json.decodeFromString<JsonObject>(capturedBody.first())
        assertEquals("""pass"word""", parsed["password"]?.jsonPrimitive?.content)
    }

    @Test
    fun `signIn with password containing backslash produces valid JSON`() = testScope.runTest {
        seedConfig()
        val capturedBody = mutableListOf<String>()
        val repo = createRepositoryCapturingBody(capturedBody)
        val result = repo.signIn("user@example.com", """pass\word""")
        assertTrue(result.isSuccess)
        val parsed = json.decodeFromString<JsonObject>(capturedBody.first())
        assertEquals("""pass\word""", parsed["password"]?.jsonPrimitive?.content)
    }

    @Test
    fun `signIn with password containing closing brace produces valid JSON`() = testScope.runTest {
        seedConfig()
        val capturedBody = mutableListOf<String>()
        val repo = createRepositoryCapturingBody(capturedBody)
        val result = repo.signIn("user@example.com", "pass}word")
        assertTrue(result.isSuccess)
        val parsed = json.decodeFromString<JsonObject>(capturedBody.first())
        assertEquals("pass}word", parsed["password"]?.jsonPrimitive?.content)
    }

    @Test
    fun `signUp with special characters in email and password produces valid JSON`() = testScope.runTest {
        seedConfig()
        val capturedBody = mutableListOf<String>()
        val repo = createRepositoryCapturingBody(capturedBody)
        val result = repo.signUp("user+test@example.com", """p@ss"w\rd}""")
        assertTrue(result.isSuccess)
        val parsed = json.decodeFromString<JsonObject>(capturedBody.first())
        assertEquals("user+test@example.com", parsed["email"]?.jsonPrimitive?.content)
        assertEquals("""p@ss"w\rd}""", parsed["password"]?.jsonPrimitive?.content)
    }

    @Test
    fun `signInWithFacebook succeeds`() = testScope.runTest {
        seedConfig()
        val repo = createRepository()
        val result = repo.signInWithFacebook("facebook-access-token")
        assertTrue(result.isSuccess)
        assertEquals(AuthState.Authenticated, repo.authState.value)
    }

    @Test
    fun `signInWithFacebook sends correct provider and token`() = testScope.runTest {
        seedConfig()
        val capturedBody = mutableListOf<String>()
        val repo = createRepositoryCapturingBody(capturedBody)
        val result = repo.signInWithFacebook("fb-token-123")
        assertTrue(result.isSuccess)
        val parsed = json.decodeFromString<JsonObject>(capturedBody.first())
        assertEquals("facebook", parsed["provider"]?.jsonPrimitive?.content)
        assertEquals("fb-token-123", parsed["id_token"]?.jsonPrimitive?.content)
    }

    @Test
    fun `signInWithFacebook fails with missing config`() = testScope.runTest {
        val repo = createRepository()
        val result = repo.signInWithFacebook("facebook-access-token")
        assertTrue(result.isFailure)
        assertEquals("Supabase URL and API Key must be configured", result.exceptionOrNull()?.message)
    }

    @Test
    fun `signInWithFacebook fails with server error`() = testScope.runTest {
        seedConfig()
        val repo = createRepository(
            statusCode = HttpStatusCode.BadRequest,
            responseBody = """{"error":"invalid_grant","error_description":"Invalid login credentials"}"""
        )
        val result = repo.signInWithFacebook("bad-fb-token")
        assertTrue(result.isFailure)
        assertEquals("The email or password you entered is incorrect", result.exceptionOrNull()?.message)
    }

    @Test
    fun `signInWithFacebook with special characters in token produces valid JSON`() = testScope.runTest {
        seedConfig()
        val capturedBody = mutableListOf<String>()
        val repo = createRepositoryCapturingBody(capturedBody)
        val result = repo.signInWithFacebook("""token"with\special}chars""")
        assertTrue(result.isSuccess)
        val parsed = json.decodeFromString<JsonObject>(capturedBody.first())
        assertEquals("""token"with\special}chars""", parsed["id_token"]?.jsonPrimitive?.content)
    }

    @Test
    fun `signInWithGoogle with special characters in token produces valid JSON`() = testScope.runTest {
        seedConfig()
        val capturedBody = mutableListOf<String>()
        val repo = createRepositoryCapturingBody(capturedBody)
        val result = repo.signInWithGoogle("""token"with\special}chars""")
        assertTrue(result.isSuccess)
        val parsed = json.decodeFromString<JsonObject>(capturedBody.first())
        assertEquals("""token"with\special}chars""", parsed["id_token"]?.jsonPrimitive?.content)
    }

    @Test
    fun `auth state flow emits transitions`() = testScope.runTest {
        seedConfig()
        val repo = createRepository()

        repo.authState.test {
            assertEquals(AuthState.Unknown, awaitItem())

            repo.refreshAuthState()
            assertEquals(AuthState.Unauthenticated, awaitItem())

            repo.signIn("user@example.com", "password")
            assertEquals(AuthState.Authenticated, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }
}
