package com.healthdispatch.ui.settings

import androidx.lifecycle.ViewModel
import com.healthdispatch.data.cloud.CloudConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    cloudConfigRepository: CloudConfigRepository
) : ViewModel() {

    val supabaseUrl = cloudConfigRepository.cloudConfigFlow.map { config ->
        config.url.ifBlank { "Not configured" }
    }
}
