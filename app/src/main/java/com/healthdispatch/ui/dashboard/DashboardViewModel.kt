package com.healthdispatch.ui.dashboard

import androidx.lifecycle.ViewModel
import com.healthdispatch.data.cloud.CloudConfigRepository
import com.healthdispatch.data.local.PendingSyncDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    cloudConfigRepository: CloudConfigRepository,
    pendingSyncDao: PendingSyncDao
) : ViewModel() {

    val syncStatus = cloudConfigRepository.cloudConfigFlow.map { config ->
        if (config.url.isBlank() || config.apiKey.isBlank()) "Paused (not configured)" else "Active"
    }

    val pendingCount = pendingSyncDao.observePendingCount().map { it.toString() }
}
