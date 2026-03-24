package com.healthdispatch.ui.setup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthdispatch.data.preferences.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val isEditMode: Boolean = savedStateHandle.get<Boolean>("editMode") ?: false

    private val _url = MutableStateFlow("")
    val url: StateFlow<String> = _url

    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey

    init {
        if (isEditMode) {
            viewModelScope.launch {
                _url.value = settingsRepository.supabaseUrl.first()
                _apiKey.value = settingsRepository.supabaseApiKey.first()
            }
        }
    }

    fun onUrlChange(value: String) { _url.value = value }
    fun onApiKeyChange(value: String) { _apiKey.value = value }

    fun saveConfig(onComplete: () -> Unit) {
        viewModelScope.launch {
            settingsRepository.saveSupabaseConfig(_url.value, _apiKey.value)
            onComplete()
        }
    }
}
