package com.healthdispatch.ui.setup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SetupScreen(
    onSetupComplete: () -> Unit,
    onSkip: () -> Unit = {},
    viewModel: SetupViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.isComplete) {
        if (state.isComplete) onSetupComplete()
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .padding(24.dp),
        ) {
            LinearProgressIndicator(
                progress = { (state.currentStep + 1).toFloat() / viewModel.totalSteps },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Step ${state.currentStep + 1} of ${viewModel.totalSteps}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedContent(
                targetState = state.currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                    } else {
                        slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                    }
                },
                modifier = Modifier.weight(1f),
                label = "wizard_step",
            ) { step ->
                when (step) {
                    0 -> WelcomeStep()
                    1 -> CloudConfigStep(
                        supabaseUrl = state.supabaseUrl,
                        supabaseKey = state.supabaseKey,
                        onUrlChange = viewModel::updateSupabaseUrl,
                        onKeyChange = viewModel::updateSupabaseKey,
                    )
                    2 -> PermissionsStep(
                        permissionsGranted = state.permissionsGranted,
                        healthConnectAvailable = state.healthConnectAvailable,
                        onPermissionsResult = { viewModel.refreshPermissions() },
                    )
                    3 -> ConfirmationStep(
                        supabaseUrl = state.supabaseUrl,
                        permissionsGranted = state.permissionsGranted,
                    )
                }
            }

            WizardNavButtons(
                currentStep = state.currentStep,
                totalSteps = viewModel.totalSteps,
                canAdvance = when (state.currentStep) {
                    1 -> state.supabaseUrl.isNotBlank() && state.supabaseKey.isNotBlank()
                    else -> true
                },
                onBack = viewModel::previousStep,
                onNext = viewModel::nextStep,
                onFinish = viewModel::completeSetup,
            )

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = onSkip) {
                Text("Skip for now")
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "You can set this up later in Settings",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WelcomeStep() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "HealthDispatch",
            style = MaterialTheme.typography.headlineLarge,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Sync your Health Connect data to your own cloud database — securely and on your terms.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "In the next few steps you'll configure your cloud endpoint, grant Health Connect permissions, and start syncing.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CloudConfigStep(
    supabaseUrl: String,
    supabaseKey: String,
    onUrlChange: (String) -> Unit,
    onKeyChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Cloud Configuration",
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Enter your Supabase project credentials. Your data is sent only to this endpoint.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = supabaseUrl,
            onValueChange = onUrlChange,
            label = { Text("Supabase URL") },
            placeholder = { Text("https://your-project.supabase.co") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = supabaseKey,
            onValueChange = onKeyChange,
            label = { Text("Supabase API Key") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
        )
    }
}

@Composable
private fun PermissionsStep(
    permissionsGranted: Boolean,
    healthConnectAvailable: Boolean,
    onPermissionsResult: () -> Unit,
) {
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
        onResult = { onPermissionsResult() },
    )

    LaunchedEffect(Unit) {
        onPermissionsResult()
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Health Connect Permissions",
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "HealthDispatch reads your health data from Health Connect to sync it to your configured cloud database. No data is shared with third parties.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(24.dp))

        val dataTypes = listOf("Steps", "Heart Rate", "Sleep", "Exercise", "Weight")
        dataTypes.forEach { type ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (permissionsGranted) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    tint = if (permissionsGranted) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Text(
                    text = type,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (!healthConnectAvailable) {
            Text(
                text = "Health Connect is not available on this device. You can still continue, but syncing will not work until Health Connect is installed.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        } else if (!permissionsGranted) {
            Button(
                onClick = {
                    permissionLauncher.launch(
                        setOf(
                            "android.permission.health.READ_STEPS",
                            "android.permission.health.READ_HEART_RATE",
                            "android.permission.health.READ_SLEEP",
                            "android.permission.health.READ_EXERCISE",
                            "android.permission.health.READ_WEIGHT",
                        )
                    )
                },
            ) {
                Text("Grant Permissions")
            }
        } else {
            Text(
                text = "All permissions granted",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun ConfirmationStep(
    supabaseUrl: String,
    permissionsGranted: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Ready to Sync",
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Review your configuration and tap Finish to start syncing.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(32.dp))

        ConfigRow(label = "Endpoint", value = supabaseUrl)

        Spacer(modifier = Modifier.height(12.dp))

        ConfigRow(
            label = "Permissions",
            value = if (permissionsGranted) "All granted" else "Not granted (sync may fail)",
        )

        Spacer(modifier = Modifier.height(12.dp))

        ConfigRow(label = "Sync interval", value = "Every 15 minutes")
    }
}

@Composable
private fun ConfigRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun WizardNavButtons(
    currentStep: Int,
    totalSteps: Int,
    canAdvance: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        if (currentStep > 0) {
            OutlinedButton(onClick = onBack) {
                Text("Back")
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        if (currentStep < totalSteps - 1) {
            Button(
                onClick = onNext,
                enabled = canAdvance,
            ) {
                Text("Next")
            }
        } else {
            Button(onClick = onFinish) {
                Text("Finish")
            }
        }
    }
}
