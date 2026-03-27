package com.healthdispatch.ui.onboarding

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private lateinit var dataStore: DataStore<Preferences>

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { File(tempFolder.newFolder(), "test_prefs.preferences_pb") }
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createHttpClient(
        statusCode: HttpStatusCode = HttpStatusCode.OK,
        responseBody: String = "[]"
    ): HttpClient {
        val mockEngine = MockEngine { _ ->
            respond(
                content = responseBody,
                status = statusCode,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        return HttpClient(mockEngine) {
            install(ContentNegotiation) { json(json) }
        }
    }

    private fun createFailingHttpClient(exception: Exception): HttpClient {
        val mockEngine = MockEngine { _ -> throw exception }
        return HttpClient(mockEngine) {
            install(ContentNegotiation) { json(json) }
        }
    }

    private fun createViewModel(httpClient: HttpClient = createHttpClient()): OnboardingViewModel {
        return OnboardingViewModel(dataStore, httpClient)
    }

    @Test
    fun `initial state is WELCOME step with empty fields`() = runTest {
        val vm = createViewModel()
        val state = vm.state.value
        assertEquals(OnboardingStep.WELCOME, state.currentStep)
        assertNull(state.pathChoice)
        assertEquals("", state.supabaseUrl)
        assertEquals("", state.supabaseKey)
        assertFalse(state.isValidating)
        assertNull(state.validationError)
    }

    @Test
    fun `goToNext advances through steps`() = runTest {
        val vm = createViewModel()
        assertEquals(OnboardingStep.WELCOME, vm.state.value.currentStep)

        vm.goToNext()
        assertEquals(OnboardingStep.PATH_CHOICE, vm.state.value.currentStep)
    }

    @Test
    fun `skip path choice skips cloud config`() = runTest {
        val vm = createViewModel()
        vm.goToNext() // WELCOME -> PATH_CHOICE
        vm.setPathChoice(PathChoice.SKIP)
        vm.goToNext() // PATH_CHOICE -> PERMISSIONS (skips CLOUD_CONFIG)
        assertEquals(OnboardingStep.PERMISSIONS, vm.state.value.currentStep)
    }

    @Test
    fun `non-skip path choice goes to cloud config`() = runTest {
        val vm = createViewModel()
        vm.goToNext() // WELCOME -> PATH_CHOICE
        vm.setPathChoice(PathChoice.CONNECT_EXISTING)
        vm.goToNext() // PATH_CHOICE -> CLOUD_CONFIG
        assertEquals(OnboardingStep.CLOUD_CONFIG, vm.state.value.currentStep)
    }

    @Test
    fun `goBack navigates backwards`() = runTest {
        val vm = createViewModel()
        vm.goToNext() // WELCOME -> PATH_CHOICE
        vm.goBack() // PATH_CHOICE -> WELCOME
        assertEquals(OnboardingStep.WELCOME, vm.state.value.currentStep)
    }

    @Test
    fun `setSupabaseUrl updates state and clears validation error`() = runTest {
        val vm = createViewModel()
        vm.setSupabaseUrl("https://test.supabase.co")
        assertEquals("https://test.supabase.co", vm.state.value.supabaseUrl)
        assertNull(vm.state.value.validationError)
    }

    @Test
    fun `setSupabaseKey updates state and clears validation error`() = runTest {
        val vm = createViewModel()
        vm.setSupabaseKey("test-key")
        assertEquals("test-key", vm.state.value.supabaseKey)
        assertNull(vm.state.value.validationError)
    }

    @Test
    fun `validateAndSaveCloudConfig succeeds and saves to DataStore`() = runTest {
        val vm = createViewModel()
        vm.goToNext() // WELCOME -> PATH_CHOICE
        vm.setPathChoice(PathChoice.CONNECT_EXISTING)
        vm.goToNext() // PATH_CHOICE -> CLOUD_CONFIG

        vm.setSupabaseUrl("https://valid.supabase.co")
        vm.setSupabaseKey("valid-key")
        vm.validateAndSaveCloudConfig()
        advanceUntilIdle()

        // Should have advanced to PERMISSIONS
        assertEquals(OnboardingStep.PERMISSIONS, vm.state.value.currentStep)
        assertFalse(vm.state.value.isValidating)
        assertNull(vm.state.value.validationError)

        // Should have saved to DataStore
        val savedUrl = dataStore.data.map { it[OnboardingViewModel.SUPABASE_URL_KEY] }.first()
        assertEquals("https://valid.supabase.co", savedUrl)
        val savedKey = dataStore.data.map { it[OnboardingViewModel.SUPABASE_API_KEY] }.first()
        assertEquals("valid-key", savedKey)
    }

    @Test
    fun `validateAndSaveCloudConfig shows error on HTTP failure`() = runTest {
        val vm = createViewModel(createHttpClient(statusCode = HttpStatusCode.Unauthorized))
        vm.goToNext()
        vm.setPathChoice(PathChoice.CONNECT_EXISTING)
        vm.goToNext()

        vm.setSupabaseUrl("https://test.supabase.co")
        vm.setSupabaseKey("bad-key")
        vm.validateAndSaveCloudConfig()
        advanceUntilIdle()

        // Should stay on CLOUD_CONFIG
        assertEquals(OnboardingStep.CLOUD_CONFIG, vm.state.value.currentStep)
        assertFalse(vm.state.value.isValidating)
        assertTrue(vm.state.value.validationError?.contains("Could not connect") == true)
    }

    @Test
    fun `validateAndSaveCloudConfig shows error on network failure`() = runTest {
        val vm = createViewModel(createFailingHttpClient(java.net.UnknownHostException("bad host")))
        vm.goToNext()
        vm.setPathChoice(PathChoice.CONNECT_EXISTING)
        vm.goToNext()

        vm.setSupabaseUrl("https://bad.supabase.co")
        vm.setSupabaseKey("key")
        vm.validateAndSaveCloudConfig()
        advanceUntilIdle()

        assertEquals(OnboardingStep.CLOUD_CONFIG, vm.state.value.currentStep)
        assertTrue(vm.state.value.validationError?.contains("Cannot reach server") == true)
    }

    @Test
    fun `validateAndSaveCloudConfig shows error on timeout`() = runTest {
        val vm = createViewModel(createFailingHttpClient(java.net.SocketTimeoutException("timeout")))
        vm.goToNext()
        vm.setPathChoice(PathChoice.CONNECT_EXISTING)
        vm.goToNext()

        vm.setSupabaseUrl("https://slow.supabase.co")
        vm.setSupabaseKey("key")
        vm.validateAndSaveCloudConfig()
        advanceUntilIdle()

        assertTrue(vm.state.value.validationError?.contains("timed out") == true)
    }

    @Test
    fun `testConnection succeeds with 200 response`() = testScope.runTest {
        val httpClient = createHttpClient(HttpStatusCode.OK)
        val vm = createViewModel(httpClient)
        val result = vm.testConnection("https://test.supabase.co", "key")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `testConnection fails with non-success status`() = testScope.runTest {
        val httpClient = createHttpClient(HttpStatusCode.Forbidden)
        val vm = createViewModel(httpClient)
        val result = vm.testConnection("https://test.supabase.co", "key")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Could not connect") == true)
    }

    @Test
    fun `testConnection fails with UnknownHostException`() = testScope.runTest {
        val httpClient = createFailingHttpClient(java.net.UnknownHostException("bad"))
        val vm = createViewModel(httpClient)
        val result = vm.testConnection("https://bad.supabase.co", "key")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Cannot reach server") == true)
    }

    @Test
    fun `testConnection fails with ConnectException`() = testScope.runTest {
        val httpClient = createFailingHttpClient(java.net.ConnectException("refused"))
        val vm = createViewModel(httpClient)
        val result = vm.testConnection("https://bad.supabase.co", "key")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Cannot reach server") == true)
    }

    @Test
    fun `testConnection fails with IOException`() = testScope.runTest {
        val httpClient = createFailingHttpClient(java.io.IOException("stream reset"))
        val vm = createViewModel(httpClient)
        val result = vm.testConnection("https://bad.supabase.co", "key")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Connection failed") == true)
    }

    @Test
    fun `completeOnboarding saves flag and updates state`() = runTest {
        val vm = createViewModel()
        vm.completeOnboarding()
        advanceUntilIdle()

        assertTrue(vm.state.value.isComplete)
        val saved = dataStore.data.map { it[OnboardingViewModel.ONBOARDING_COMPLETE_KEY] }.first()
        assertTrue(saved == true)
    }

    @Test
    fun `clearValidationError clears the error`() = runTest {
        val vm = createViewModel(createFailingHttpClient(java.net.UnknownHostException("bad")))
        vm.setSupabaseUrl("https://bad.supabase.co")
        vm.setSupabaseKey("key")
        vm.validateAndSaveCloudConfig()
        advanceUntilIdle()

        assertTrue(vm.state.value.validationError != null)
        vm.clearValidationError()
        assertNull(vm.state.value.validationError)
    }
}
