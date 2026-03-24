package com.healthdispatch.ui.onboarding

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun CloudConfigStep(
    pathChoice: PathChoice?,
    supabaseUrl: String,
    supabaseKey: String,
    onUrlChange: (String) -> Unit,
    onKeyChange: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val title = when (pathChoice) {
        PathChoice.CONNECT_EXISTING -> "Connect to your cloud"
        PathChoice.SETUP_NEW -> "Configure your new cloud"
        else -> "Cloud configuration"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Enter your Supabase project details",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = supabaseUrl,
            onValueChange = onUrlChange,
            label = { Text("Supabase URL") },
            placeholder = { Text("https://your-project.supabase.co") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = supabaseKey,
            onValueChange = onKeyChange,
            label = { Text("Supabase API Key") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )

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
                enabled = supabaseUrl.isNotBlank() && supabaseKey.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                Text("Next")
            }
        }
    }
}
