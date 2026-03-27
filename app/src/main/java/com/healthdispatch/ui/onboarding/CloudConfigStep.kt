package com.healthdispatch.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.healthdispatch.data.cloud.SupabaseProject

@Composable
fun CloudConfigStep(
    onBack: () -> Unit,
    onComplete: () -> Unit,
    viewModel: CloudConfigWizardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    AnimatedContent(
        targetState = state.step,
        modifier = Modifier.fillMaxSize(),
        label = "wizard_step"
    ) { step ->
        when (step) {
            WizardStep.SIGN_IN -> SignInStep(
                isLoading = state.isLoading,
                error = state.error,
                onSignIn = { token -> viewModel.onAccessTokenReceived(token) },
                onBack = onBack
            )

            WizardStep.SELECT_PROJECT -> SelectProjectStep(
                projects = state.projects,
                isLoading = state.isLoading,
                error = state.error,
                onSelectProject = { viewModel.selectProject(it) },
                onCreateNew = { viewModel.goToCreateProject() },
                onBack = onBack
            )

            WizardStep.CREATE_PROJECT -> CreateProjectStep(
                organizations = state.organizations,
                isLoading = state.isLoading,
                error = state.error,
                onCreateProject = { orgId -> viewModel.createNewProject(orgId) },
                onBack = {
                    if (state.projects.isNotEmpty()) viewModel.goBackToProjectList()
                    else onBack()
                }
            )

            WizardStep.SETTING_UP -> SettingUpStep()

            WizardStep.COMPLETE -> WizardCompleteStep(onContinue = onComplete)
        }
    }
}

@Composable
private fun SignInStep(
    isLoading: Boolean,
    error: String?,
    onSignIn: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.Cloud,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Set up your cloud",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Sign in to your Supabase account to automatically set up your private cloud storage.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = { onSignIn("placeholder-token") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sign in to Supabase")
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { onSignIn("placeholder-token") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create Supabase Account")
            }
        }

        error?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        TextButton(onClick = onBack) {
            Text("Back")
        }
    }
}

@Composable
private fun SelectProjectStep(
    projects: List<SupabaseProject>,
    isLoading: Boolean,
    error: String?,
    onSelectProject: (SupabaseProject) -> Unit,
    onCreateNew: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Select a project",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Choose an existing Supabase project or create a new one.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(projects) { project ->
                    Card(
                        onClick = { onSelectProject(project) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Cloud,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = project.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = project.region,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedButton(
                        onClick = onCreateNew,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create new project")
                    }
                }
            }
        }

        error?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }
}

@Composable
private fun CreateProjectStep(
    organizations: List<com.healthdispatch.data.cloud.SupabaseOrganization>,
    isLoading: Boolean,
    error: String?,
    onCreateProject: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Create your cloud project",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "We'll create a private Supabase project for your health data. Your data stays under your control.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (isLoading) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Setting up your project...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            if (organizations.isNotEmpty()) {
                organizations.forEach { org ->
                    Button(
                        onClick = { onCreateProject(org.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Create project in ${org.name}")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else {
                Button(
                    onClick = { onCreateProject("") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Create project")
                }
            }
        }

        error?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (!isLoading) {
            TextButton(onClick = onBack) {
                Text("Back")
            }
        }
    }
}

@Composable
private fun SettingUpStep() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(modifier = Modifier.size(48.dp))

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Setting up your cloud...",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Creating your project, setting up database tables, and configuring security policies.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun WizardCompleteStep(onContinue: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Cloud ready!",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your private cloud storage is configured. Your health data will sync securely to your own project.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue")
        }
    }
}
