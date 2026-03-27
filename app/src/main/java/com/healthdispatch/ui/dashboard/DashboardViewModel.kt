package com.healthdispatch.ui.dashboard

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.healthdispatch.data.local.PendingSyncDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    firebaseAuth: FirebaseAuth,
    pendingSyncDao: PendingSyncDao
) : ViewModel() {

    val syncStatus = MutableStateFlow(
        if (firebaseAuth.currentUser != null) "Active" else "Paused (not signed in)"
    )

    val pendingCount = pendingSyncDao.observePendingCount().map { it.toString() }
}
