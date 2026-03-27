package com.healthdispatch.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthdispatch.data.auth.AuthRepository
import com.healthdispatch.data.auth.AuthState
import com.healthdispatch.data.cloud.CloudConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NavViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val cloudConfigRepository: CloudConfigRepository
) : ViewModel() {

    val authState: StateFlow<AuthState> = authRepository.authState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AuthState.Unknown)

    val onboardingComplete: StateFlow<Boolean> = cloudConfigRepository.onboardingCompleteFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    init {
        viewModelScope.launch {
            authRepository.refreshAuthState()
        }
    }
}
