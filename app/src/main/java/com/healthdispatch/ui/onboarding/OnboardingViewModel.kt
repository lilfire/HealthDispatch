package com.healthdispatch.ui.onboarding

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class OnboardingStep { WELCOME, PATH_CHOICE, CLOUD_CONFIG, PERMISSIONS, DONE }

enum class PathChoice { CONNECT_EXISTING, SETUP_NEW, SKIP }

data class OnboardingState(
    val currentStep: OnboardingStep = OnboardingStep.WELCOME,
    val pathChoice: PathChoice? = null,
    val supabaseUrl: String = "",
    val supabaseKey: String = "",
    val permissionsGranted: Boolean = false,
    val isComplete: Boolean = false,
    val isValidating: Boolean = false,
    val validationError: String? = null
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val httpClient: HttpClient
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    val stepIndex: Int
        get() = _state.value.currentStep.ordinal

    val totalSteps: Int = OnboardingStep.entries.size

    fun goToNext() {
        _state.update { current ->
            val nextStep = when (current.currentStep) {
                OnboardingStep.WELCOME -> OnboardingStep.PATH_CHOICE
                OnboardingStep.PATH_CHOICE -> {
                    if (current.pathChoice == PathChoice.SKIP) OnboardingStep.PERMISSIONS
                    else OnboardingStep.CLOUD_CONFIG
                }
                OnboardingStep.CLOUD_CONFIG -> OnboardingStep.PERMISSIONS
                OnboardingStep.PERMISSIONS -> OnboardingStep.DONE
                OnboardingStep.DONE -> return
            }
            current.copy(currentStep = nextStep)
        }
    }

    fun goBack() {
        _state.update { current ->
            val prevStep = when (current.currentStep) {
                OnboardingStep.WELCOME -> return
                OnboardingStep.PATH_CHOICE -> OnboardingStep.WELCOME
                OnboardingStep.CLOUD_CONFIG -> OnboardingStep.PATH_CHOICE
                OnboardingStep.PERMISSIONS -> {
                    if (current.pathChoice == PathChoice.SKIP) OnboardingStep.PATH_CHOICE
                    else OnboardingStep.CLOUD_CONFIG
                }
                OnboardingStep.DONE -> OnboardingStep.PERMISSIONS
            }
            current.copy(currentStep = prevStep)
        }
    }

    fun setPathChoice(choice: PathChoice) {
        _state.update { it.copy(pathChoice = choice) }
    }

    fun setSupabaseUrl(url: String) {
        _state.update { it.copy(supabaseUrl = url, validationError = null) }
    }

    fun setSupabaseKey(key: String) {
        _state.update { it.copy(supabaseKey = key, validationError = null) }
    }

    fun setPermissionsGranted(granted: Boolean) {
        _state.update { it.copy(permissionsGranted = granted) }
    }

    fun clearValidationError() {
        _state.update { it.copy(validationError = null) }
    }

    fun validateAndSaveCloudConfig() {
        val current = _state.value
        if (current.isValidating) return

        _state.update { it.copy(isValidating = true, validationError = null) }

        viewModelScope.launch {
            val result = testConnection(current.supabaseUrl, current.supabaseKey)
            result.onSuccess {
                dataStore.edit { prefs ->
                    prefs[SUPABASE_URL_KEY] = current.supabaseUrl
                    prefs[SUPABASE_API_KEY] = current.supabaseKey
                }
                _state.update { it.copy(isValidating = false) }
                goToNext()
            }
            result.onFailure { error ->
                _state.update {
                    it.copy(
                        isValidating = false,
                        validationError = error.message ?: "Connection failed"
                    )
                }
            }
        }
    }

    internal suspend fun testConnection(url: String, apiKey: String): Result<Unit> {
        return try {
            val response = httpClient.get("$url/rest/v1/") {
                headers {
                    append("apikey", apiKey)
                }
            }
            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Could not connect to Supabase (${response.status}). Check your URL and API key"))
            }
        } catch (e: java.net.UnknownHostException) {
            Result.failure(Exception("Cannot reach server. Check the URL and your internet connection"))
        } catch (e: java.net.ConnectException) {
            Result.failure(Exception("Cannot reach server. Check the URL and your internet connection"))
        } catch (e: java.net.SocketTimeoutException) {
            Result.failure(Exception("Connection timed out. Check the URL and your internet connection"))
        } catch (e: java.io.IOException) {
            Result.failure(Exception("Connection failed. Check the URL and your internet connection"))
        } catch (e: Exception) {
            Result.failure(Exception("Connection failed: ${e.message}"))
        }
    }

    fun saveCloudConfig() {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[SUPABASE_URL_KEY] = _state.value.supabaseUrl
                prefs[SUPABASE_API_KEY] = _state.value.supabaseKey
            }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[ONBOARDING_COMPLETE_KEY] = true
            }
            _state.update { it.copy(isComplete = true) }
        }
    }

    companion object {
        val SUPABASE_URL_KEY = stringPreferencesKey("supabase_url")
        val SUPABASE_API_KEY = stringPreferencesKey("supabase_api_key")
        val ONBOARDING_COMPLETE_KEY = booleanPreferencesKey("onboarding_complete")
    }
}
