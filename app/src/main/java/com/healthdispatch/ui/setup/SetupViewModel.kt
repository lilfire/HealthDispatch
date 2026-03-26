package com.healthdispatch.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthdispatch.data.auth.AuthClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SetupUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isAuthenticated: Boolean = false
)

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val authClient: AuthClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password) }
    }

    fun signIn() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank() || state.isLoading) return

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = authClient.signInWithEmail(state.email, state.password)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, isAuthenticated = true) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Sign in failed"
                        )
                    }
                }
            )
        }
    }
}
