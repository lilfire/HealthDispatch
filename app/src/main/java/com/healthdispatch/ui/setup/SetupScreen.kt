package com.healthdispatch.ui.setup

import android.webkit.URLUtil
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

private const val MIN_API_KEY_LENGTH = 20

internal fun validateSupabaseUrl(url: String): String? {
    if (url.isBlank()) return null // no error while empty
    if (!URLUtil.isHttpsUrl(url)) return "URL must start with https://"
    if (!url.contains(".supabase.co")) return "URL should contain .supabase.co"
    return null
}

internal fun validateApiKey(key: String): String? {
    if (key.isBlank()) return null
    if (key.length < MIN_API_KEY_LENGTH) return "API key must be at least $MIN_API_KEY_LENGTH characters"
    return null
}

@Composable
fun SetupScreen(onSetupComplete: () -> Unit) {
    var supabaseUrl by remember { mutableStateOf("") }
    var supabaseKey by remember { mutableStateOf("") }
    var urlTouched by remember { mutableStateOf(false) }
    var keyTouched by remember { mutableStateOf(false) }

    val urlError = if (urlTouched) validateSupabaseUrl(supabaseUrl) else null
    val keyError = if (keyTouched) validateApiKey(supabaseKey) else null

    val isFormValid by remember {
        derivedStateOf {
            supabaseUrl.isNotBlank() &&
                validateSupabaseUrl(supabaseUrl) == null &&
                supabaseKey.isNotBlank() &&
                validateApiKey(supabaseKey) == null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "HealthDispatch",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Configure your cloud endpoint",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = supabaseUrl,
            onValueChange = {
                supabaseUrl = it
                urlTouched = true
            },
            label = { Text("Supabase URL") },
            placeholder = { Text("https://your-project.supabase.co") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = urlError != null,
            supportingText = urlError?.let { error -> { Text(error) } }
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = supabaseKey,
            onValueChange = {
                supabaseKey = it
                keyTouched = true
            },
            label = { Text("Supabase API Key") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            isError = keyError != null,
            supportingText = keyError?.let { error -> { Text(error) } }
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                // TODO: Save to DataStore, request HC permissions, start sync
                onSetupComplete()
            },
            enabled = isFormValid,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Connect & Start Syncing")
        }
    }
}
