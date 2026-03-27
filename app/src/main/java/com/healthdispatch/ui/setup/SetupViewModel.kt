package com.healthdispatch.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthdispatch.BuildConfig
import com.healthdispatch.data.auth.AuthRepository
import com.healthdispatch.data.auth.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SetupUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isSignUpMode: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val googleSignInAvailable: Boolean = false
)

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    val googleClientId: String = BuildConfig.GOOGLE_CLIENT_ID

    private val _uiState = MutableStateFlow(
        SetupUiState(googleSignInAvailable = googleClientId.isNotBlank())
    )
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    private val _authSuccessEvent = Channel<Boolean>(Channel.BUFFERED)
    val authSuccessEvent = _authSuccessEvent.receiveAsFlow()

    init {
        viewModelScope.launch {
            authRepository.refreshAuthState()
        }
        viewModelScope.launch {
            authRepository.authState.collect { state ->
                if (state is AuthState.Authenticated) {
                    _authSuccessEvent.send(true)
                }
            }
        }
    }

    fun updateEmail(email: String) {
        _uiState.update { it.copy(email = email) }
    }

    fun updatePassword(password: String) {
        _uiState.update { it.copy(password = password) }
    }

    fun updateConfirmPassword(confirmPassword: String) {
        _uiState.update { it.copy(confirmPassword = confirmPassword) }
    }

    fun toggleMode() {
        _uiState.update {
            it.copy(
                isSignUpMode = !it.isSignUpMode,
                errorMessage = null,
                confirmPassword = ""
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun submit() {
        val state = _uiState.value
        if (state.isLoading) return

        val validationError = validate(state)
        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = if (state.isSignUpMode) {
                authRepository.signUp(state.email, state.password)
            } else {
                authRepository.signIn(state.email, state.password)
            }

            result.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Authentication failed"
                    )
                }
            }
            result.onSuccess {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun handleGoogleSignIn(idToken: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = authRepository.signInWithGoogle(idToken)
            result.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Google sign-in failed"
                    )
                }
            }
            result.onSuccess {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun handleAppleSignIn(idToken: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = authRepository.signInWithApple(idToken)
            result.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Apple sign-in failed"
                    )
                }
            }
            result.onSuccess {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun handleFacebookSignIn(accessToken: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = authRepository.signInWithFacebook(accessToken)
            result.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Facebook sign-in failed"
                    )
                }
            }
            result.onSuccess {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun validate(state: SetupUiState): String? {
        if (state.email.isBlank()) {
            return "Please enter your email address"
        }
        if (state.password.isBlank()) {
            return "Please enter your password"
        }
        if (!isValidEmail(state.email)) {
            return "Please enter a valid email address"
        }
        if (state.isSignUpMode) {
            if (state.password.length < 6) {
                return "Password must be at least 6 characters"
            }
            if (state.password != state.confirmPassword) {
                return "Passwords do not match"
            }
        }
        return null
    }

    private fun isValidEmail(email: String): Boolean {
        return EMAIL_PATTERN.matches(email)
    }

    companion object {
        private val EMAIL_PATTERN = Regex(
            "[a-zA-Z0-9+._%\\-]{1,256}@[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}(\\.[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25})+"
        )
    }
}
