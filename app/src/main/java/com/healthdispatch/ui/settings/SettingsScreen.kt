package com.healthdispatch.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onRerunSetup: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    var editingUrl by remember { mutableStateOf(false) }
    var editingApiKey by remember { mutableStateOf(false) }
    var urlDraft by remember(state.supabaseUrl) { mutableStateOf(state.supabaseUrl) }
    var apiKeyDraft by remember(state.apiKey) { mutableStateOf(state.apiKey) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ListItem(
                headlineContent = { Text("Supabase URL") },
                supportingContent = {
                    Text(state.supabaseUrl.ifBlank { "Not configured" })
                },
                modifier = Modifier.clickable {
                    urlDraft = state.supabaseUrl
                    editingUrl = true
                }
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Supabase API Key") },
                supportingContent = {
                    Text(maskApiKey(state.apiKey))
                },
                modifier = Modifier.clickable {
                    apiKeyDraft = state.apiKey
                    editingApiKey = true
                }
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Sync Interval") },
                supportingContent = { Text("15 minutes") }
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Data Types") },
                supportingContent = { Text("Steps, Heart Rate, Sleep, Exercise, Weight") }
            )
            HorizontalDivider()

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = {
                    viewModel.resetOnboarding()
                    onRerunSetup()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text("Re-run Setup Wizard")
            }
        }
    }

    if (editingUrl) {
        EditDialog(
            title = "Supabase URL",
            value = urlDraft,
            onValueChange = { urlDraft = it },
            onDismiss = { editingUrl = false },
            onSave = {
                viewModel.saveUrl(urlDraft)
                editingUrl = false
            }
        )
    }

    if (editingApiKey) {
        EditDialog(
            title = "Supabase API Key",
            value = apiKeyDraft,
            onValueChange = { apiKeyDraft = it },
            isPassword = true,
            onDismiss = { editingApiKey = false },
            onSave = {
                viewModel.saveApiKey(apiKeyDraft)
                editingApiKey = false
            }
        )
    }
}

@Composable
private fun EditDialog(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    isPassword: Boolean = false,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit $title") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(title) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None
            )
        },
        confirmButton = {
            Button(onClick = onSave, enabled = value.isNotBlank()) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun maskApiKey(apiKey: String): String {
    if (apiKey.isBlank()) return "Not configured"
    if (apiKey.length <= 8) return "••••••••"
    return apiKey.take(4) + "••••" + apiKey.takeLast(4)
}
