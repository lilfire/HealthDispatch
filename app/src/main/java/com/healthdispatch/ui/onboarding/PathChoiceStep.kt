package com.healthdispatch.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PathChoiceStep(
    onChoice: (PathChoice) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "How would you like to start?",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedButton(
            onClick = { onChoice(PathChoice.CONNECT_EXISTING) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Connect to existing cloud")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { onChoice(PathChoice.SETUP_NEW) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Set up new cloud")
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(
            onClick = { onChoice(PathChoice.SKIP) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Skip for now")
        }
    }
}
