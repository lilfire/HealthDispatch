package com.healthdispatch.ui.onboarding

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val permissionsGranted: Boolean = false,
    val isComplete: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>
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

    fun setPermissionsGranted(granted: Boolean) {
        _state.update { it.copy(permissionsGranted = granted) }
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
        val ONBOARDING_COMPLETE_KEY = booleanPreferencesKey("onboarding_complete")
    }
}
