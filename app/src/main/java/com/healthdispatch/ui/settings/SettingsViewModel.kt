package com.healthdispatch.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthdispatch.data.cloud.CloudConfig
import com.healthdispatch.data.cloud.CloudConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val supabaseUrl: String = "",
    val apiKey: String = "",
    val isConfigured: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val cloudConfigRepository: CloudConfigRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = cloudConfigRepository.cloudConfigFlow
        .map { config ->
            SettingsUiState(
                supabaseUrl = config.url,
                apiKey = config.apiKey,
                isConfigured = config.url.isNotBlank() && config.apiKey.isNotBlank()
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun saveUrl(url: String) {
        viewModelScope.launch {
            val current = cloudConfigRepository.currentConfig()
            cloudConfigRepository.saveCloudConfig(url, current.apiKey)
        }
    }

    fun saveApiKey(apiKey: String) {
        viewModelScope.launch {
            val current = cloudConfigRepository.currentConfig()
            cloudConfigRepository.saveCloudConfig(current.url, apiKey)
        }
    }

    fun resetOnboarding() {
        viewModelScope.launch {
            cloudConfigRepository.resetOnboarding()
        }
    }
}
