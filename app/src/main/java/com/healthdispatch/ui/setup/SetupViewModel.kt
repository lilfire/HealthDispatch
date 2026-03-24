package com.healthdispatch.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthdispatch.data.cloud.CloudConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val cloudConfigRepository: CloudConfigRepository
) : ViewModel() {

    fun saveConfig(url: String, apiKey: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            cloudConfigRepository.saveCloudConfig(url, apiKey)
            onComplete()
        }
    }
}
