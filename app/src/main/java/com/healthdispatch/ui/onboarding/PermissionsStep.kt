package com.healthdispatch.ui.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController

@Composable
fun PermissionsStep(
    permissionsGranted: Boolean,
    requiredPermissions: Set<String>,
    onPermissionsResult: (Boolean) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        onPermissionsResult(granted.containsAll(requiredPermissions))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Health Connect Permissions",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "HealthDispatch needs access to your health data to sync it. We read steps, heart rate, sleep, exercise, and weight records.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (permissionsGranted) {
            Text(
                text = "Permissions granted",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Button(
                onClick = { permissionLauncher.launch(requiredPermissions) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Grant Permissions")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }

            Spacer(modifier = Modifier.width(12.dp))

            Button(
                onClick = onNext,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (permissionsGranted) "Next" else "Skip")
            }
        }
    }
}
