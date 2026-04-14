package com.healthdispatch.ui.setup

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// TODO: Fix Robolectric Compose test infrastructure — ComponentActivity not resolved
//  in merged manifest. Requires AGP/Robolectric manifest discovery investigation.
@Ignore("Robolectric cannot resolve ComponentActivity — needs test infra fix")
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class SetupScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(
        uiState: SetupUiState = SetupUiState(),
        onGoogleSignIn: () -> Unit = {},
        onFacebookSignIn: () -> Unit = {},
        onClearError: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            SetupScreenContent(
                uiState = uiState,
                onGoogleSignIn = onGoogleSignIn,
                onFacebookSignIn = onFacebookSignIn,
                onClearError = onClearError
            )
        }
    }

    @Test
    fun setupScreen_displaysTitle() {
        setContent()
        composeTestRule.onNodeWithText("HealthDispatch").assertIsDisplayed()
    }

    @Test
    fun setupScreen_displaysGoogleSignInButton() {
        setContent()
        composeTestRule.onNodeWithText("Sign in with Google").assertIsDisplayed()
    }

    @Test
    fun setupScreen_displaysFacebookSignInButton() {
        setContent()
        composeTestRule.onNodeWithText("Sign in with Facebook").assertIsDisplayed()
    }

    @Test
    fun setupScreen_googleSignInTriggersCallback() {
        var invoked = false
        setContent(onGoogleSignIn = { invoked = true })
        composeTestRule.onNodeWithText("Sign in with Google").performClick()
        assertTrue("onGoogleSignIn callback should have been invoked", invoked)
    }

    @Test
    fun setupScreen_facebookSignInTriggersCallback() {
        var invoked = false
        setContent(onFacebookSignIn = { invoked = true })
        composeTestRule.onNodeWithText("Sign in with Facebook").performClick()
        assertTrue("onFacebookSignIn callback should have been invoked", invoked)
    }

    @Test
    fun setupScreen_displaysErrorMessage() {
        setContent(uiState = SetupUiState(errorMessage = "Invalid credentials"))
        composeTestRule.onNodeWithText("Invalid credentials").assertIsDisplayed()
    }
}
