package com.healthdispatch.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthdispatch.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isSigningOut: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _signOutEvent = Channel<Boolean>(Channel.BUFFERED)
    val signOutEvent = _signOutEvent.receiveAsFlow()

    fun signOut() {
        if (_uiState.value.isSigningOut) return

        _uiState.update { it.copy(isSigningOut = true, errorMessage = null) }

        viewModelScope.launch {
            val result = authRepository.signOut()
            result.onSuccess {
                _signOutEvent.send(true)
            }
            result.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSigningOut = false,
                        errorMessage = error.message ?: "Sign out failed"
                    )
                }
            }
            _uiState.update { it.copy(isSigningOut = false) }
        }
    }
}
