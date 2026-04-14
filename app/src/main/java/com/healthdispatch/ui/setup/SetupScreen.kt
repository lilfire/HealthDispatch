package com.healthdispatch.ui.setup

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.healthdispatch.BuildConfig
import com.healthdispatch.R
import kotlinx.coroutines.launch

@Composable
fun SetupScreen(
    onSetupComplete: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.authSuccessEvent.collect {
            onSetupComplete()
        }
    }

    val callbackManager = remember { CallbackManager.Factory.create() }

    DisposableEffect(callbackManager) {
        LoginManager.getInstance().registerCallback(
            callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    viewModel.handleFacebookSignIn(result.accessToken.token)
                }

                override fun onCancel() {
                    // user cancelled — silent
                }

                override fun onError(error: FacebookException) {
                    viewModel.setError("Facebook sign-in failed. Please try again.")
                }
            }
        )
        onDispose {
            LoginManager.getInstance().unregisterCallback(callbackManager)
        }
    }

    SetupScreenContent(
        uiState = uiState,
        onGoogleSignIn = {
            coroutineScope.launch {
                try {
                    val googleIdOption = GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(BuildConfig.GOOGLE_CLIENT_ID)
                        .build()
                    val request = GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()
                    val credentialManager = CredentialManager.create(context)
                    val credentialResponse = credentialManager.getCredential(context, request)
                    val googleCredential = GoogleIdTokenCredential.createFrom(
                        credentialResponse.credential.data
                    )
                    viewModel.handleGoogleSignIn(googleCredential.idToken)
                } catch (e: GetCredentialCancellationException) {
                    // user cancelled — silent
                } catch (e: GetCredentialException) {
                    viewModel.setError("Google sign-in failed. Please try again.")
                } catch (e: Exception) {
                    viewModel.setError("Google sign-in failed. Please try again.")
                }
            }
        },
        onFacebookSignIn = {
            val activity = context as? Activity
            if (activity != null) {
                LoginManager.getInstance().logIn(
                    activity as androidx.activity.result.ActivityResultRegistryOwner,
                    callbackManager,
                    listOf("public_profile", "email")
                )
            }
        },
        onClearError = viewModel::clearError
    )
}

@Composable
fun SetupScreenContent(
    uiState: SetupUiState,
    onGoogleSignIn: () -> Unit,
    onFacebookSignIn: () -> Unit,
    onClearError: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "HealthDispatch",
                    style = MaterialTheme.typography.headlineLarge
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Sign in to continue",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Google Sign-In button
                OutlinedButton(
                    onClick = {
                        onClearError()
                        onGoogleSignIn()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = !uiState.isLoading
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_google),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.Unspecified
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                    Text("Sign in with Google")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Facebook Sign-In button
                Button(
                    onClick = {
                        onClearError()
                        onFacebookSignIn()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                    enabled = !uiState.isLoading
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_facebook),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.Unspecified
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                    Text("Sign in with Facebook", color = Color.White)
                }

                // Error display
                AnimatedVisibility(visible = uiState.errorMessage != null) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = uiState.errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.semantics {
                                liveRegion = LiveRegionMode.Polite
                            }
                        )
                    }
                }

                // Loading indicator
                AnimatedVisibility(visible = uiState.isLoading) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                }
            }
        }
    }
}
