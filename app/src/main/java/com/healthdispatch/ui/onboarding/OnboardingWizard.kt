package com.healthdispatch.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.healthdispatch.data.healthconnect.HealthConnectRepository

@Composable
fun OnboardingWizard(
    healthConnectRepository: HealthConnectRepository,
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        LinearProgressIndicator(
            progress = { (state.currentStep.ordinal + 1).toFloat() / viewModel.totalSteps },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Text(
            text = "Step ${state.currentStep.ordinal + 1} of ${viewModel.totalSteps}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 8.dp)
        )

        AnimatedContent(
            targetState = state.currentStep,
            transitionSpec = {
                if (targetState.ordinal > initialState.ordinal) {
                    slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                } else {
                    slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                }
            },
            modifier = Modifier.fillMaxSize(),
            label = "onboarding_step"
        ) { step ->
            when (step) {
                OnboardingStep.WELCOME -> WelcomeStep(
                    onContinue = { viewModel.goToNext() }
                )

                OnboardingStep.PERMISSIONS -> PermissionsStep(
                    permissionsGranted = state.permissionsGranted,
                    requiredPermissions = healthConnectRepository.requiredPermissions,
                    onPermissionsResult = { viewModel.setPermissionsGranted(it) },
                    onBack = { viewModel.goBack() },
                    onNext = { viewModel.goToNext() }
                )

                OnboardingStep.DONE -> DoneStep(
                    onFinish = {
                        viewModel.completeOnboarding()
                        onComplete()
                    }
                )
            }
        }
    }
}
