package com.healthdispatch.ui.setup

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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun SetupScreen(onSetupComplete: () -> Unit, onSkip: () -> Unit) {
    var supabaseUrl by remember { mutableStateOf("") }
    var supabaseKey by remember { mutableStateOf("") }

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
            onValueChange = { supabaseUrl = it },
            label = { Text("Supabase URL") },
            placeholder = { Text("https://your-project.supabase.co") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = supabaseKey,
            onValueChange = { supabaseKey = it },
            label = { Text("Supabase API Key") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                // TODO: Save to DataStore, request HC permissions, start sync
                onSetupComplete()
            },
            enabled = supabaseUrl.isNotBlank() && supabaseKey.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Connect & Start Syncing")
        }

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
