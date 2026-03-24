package com.healthdispatch.ui.setup

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthdispatch.data.healthconnect.HealthConnectRepository
import com.healthdispatch.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SetupUiState(
    val currentStep: Int = 0,
    val supabaseUrl: String = "",
    val supabaseKey: String = "",
    val permissionsGranted: Boolean = false,
    val healthConnectAvailable: Boolean = true,
    val isComplete: Boolean = false,
)

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val healthConnectRepo: HealthConnectRepository,
    private val syncScheduler: SyncScheduler,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    val totalSteps = 4

    init {
        checkHealthConnectAvailability()
    }

    private fun checkHealthConnectAvailability() {
        viewModelScope.launch {
            val available = healthConnectRepo.isAvailable()
            _uiState.update { it.copy(healthConnectAvailable = available) }
        }
    }

    fun nextStep() {
        _uiState.update { it.copy(currentStep = (it.currentStep + 1).coerceAtMost(totalSteps - 1)) }
    }

    fun previousStep() {
        _uiState.update { it.copy(currentStep = (it.currentStep - 1).coerceAtLeast(0)) }
    }

    fun updateSupabaseUrl(url: String) {
        _uiState.update { it.copy(supabaseUrl = url) }
    }

    fun updateSupabaseKey(key: String) {
        _uiState.update { it.copy(supabaseKey = key) }
    }

    fun refreshPermissions() {
        viewModelScope.launch {
            val granted = healthConnectRepo.hasAllPermissions()
            _uiState.update { it.copy(permissionsGranted = granted) }
        }
    }

    fun completeSetup() {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[KEY_SUPABASE_URL] = _uiState.value.supabaseUrl.trim()
                prefs[KEY_SUPABASE_KEY] = _uiState.value.supabaseKey.trim()
            }
            syncScheduler.startPeriodicSync()
            syncScheduler.startForegroundService()
            _uiState.update { it.copy(isComplete = true) }
        }
    }

    companion object {
        val KEY_SUPABASE_URL = stringPreferencesKey("supabase_url")
        val KEY_SUPABASE_KEY = stringPreferencesKey("supabase_api_key")
    }
}
