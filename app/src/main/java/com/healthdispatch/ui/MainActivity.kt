package com.healthdispatch.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.healthdispatch.data.cloud.CloudConfigRepository
import com.healthdispatch.data.healthconnect.HealthConnectRepository
import com.healthdispatch.ui.navigation.HealthDispatchNavHost
import com.healthdispatch.ui.theme.HealthDispatchTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var healthConnectRepository: HealthConnectRepository

    @Inject
    lateinit var cloudConfigRepository: CloudConfigRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HealthDispatchTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HealthDispatchNavHost(
                        healthConnectRepository = healthConnectRepository,
                        cloudConfigRepository = cloudConfigRepository
                    )
                }
            }
        }
    }
}
