package com.healthdispatch.ui.permission

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthdispatch.data.healthconnect.HealthConnectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HealthPermissionUiState(
    val healthConnectAvailable: Boolean = true,
    val allGranted: Boolean = false
)

@HiltViewModel
class HealthPermissionViewModel @Inject constructor(
    private val healthConnectRepository: HealthConnectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HealthPermissionUiState())
    val uiState: StateFlow<HealthPermissionUiState> = _uiState.asStateFlow()

    val requiredPermissions: Set<String> = healthConnectRepository.requiredPermissions

    init {
        viewModelScope.launch {
            val available = try {
                healthConnectRepository.isAvailable()
            } catch (_: Exception) {
                false
            }
            val granted = if (available) {
                try {
                    healthConnectRepository.hasAllPermissions()
                } catch (_: Exception) {
                    false
                }
            } else {
                false
            }
            _uiState.update {
                it.copy(healthConnectAvailable = available, allGranted = granted)
            }
        }
    }

    fun onPermissionResult(onGranted: () -> Unit) {
        viewModelScope.launch {
            val allGranted = try {
                healthConnectRepository.hasAllPermissions()
            } catch (_: Exception) {
                false
            }
            _uiState.update { it.copy(allGranted = allGranted) }
            if (allGranted) {
                onGranted()
            }
        }
    }
}
