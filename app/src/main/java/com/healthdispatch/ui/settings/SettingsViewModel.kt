package com.healthdispatch.ui.settings

import androidx.lifecycle.ViewModel
import com.healthdispatch.data.preferences.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    settingsRepository: SettingsRepository
) : ViewModel() {
    val supabaseUrl: Flow<String> = settingsRepository.supabaseUrl
}
