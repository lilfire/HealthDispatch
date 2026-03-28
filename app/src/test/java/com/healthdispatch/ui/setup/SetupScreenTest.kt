package com.healthdispatch.ui.setup

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class SetupScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setContent(uiState: SetupUiState = SetupUiState(), callbacks: TestCallbacks = TestCallbacks()) {
        composeTestRule.setContent {
            SetupScreenContent(
                uiState = uiState,
                onEmailChange = callbacks.onEmailChange,
                onPasswordChange = callbacks.onPasswordChange,
                onConfirmPasswordChange = callbacks.onConfirmPasswordChange,
                onToggleMode = callbacks.onToggleMode,
                onSubmit = callbacks.onSubmit,
                onClearError = callbacks.onClearError
            )
        }
    }

    @Test
    fun setupScreen_displaysBothInputFields() {
        setContent()
        composeTestRule.onNode(hasText("Email") and hasSetTextAction())
            .assertIsDisplayed()
        composeTestRule.onNode(hasText("Password") and hasSetTextAction())
            .assertIsDisplayed()
    }

    @Test
    fun setupScreen_displaysTitle() {
        setContent()
        composeTestRule.onNodeWithText("HealthDispatch").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sign in to your account").assertIsDisplayed()
    }

    @Test
    fun setupScreen_signInButtonEnabledByDefault() {
        setContent()
        composeTestRule.onNodeWithText("Sign In").assertIsEnabled()
    }

    @Test
    fun setupScreen_signInButtonDisabledWhenLoading() {
        setContent(uiState = SetupUiState(isLoading = true))
        composeTestRule.onNodeWithText("Sign In").assertDoesNotExist()
    }

    @Test
    fun setupScreen_showsSignUpSubtitleInSignUpMode() {
        setContent(uiState = SetupUiState(isSignUpMode = true))
        composeTestRule.onNodeWithText("Create your account").assertIsDisplayed()
    }

    @Test
    fun setupScreen_showsCreateAccountButtonInSignUpMode() {
        setContent(uiState = SetupUiState(isSignUpMode = true))
        composeTestRule.onNodeWithText("Create Account").assertIsDisplayed()
    }

    @Test
    fun setupScreen_submitButtonTriggersCallback() {
        var submitInvoked = false
        val callbacks = TestCallbacks(onSubmit = { submitInvoked = true })
        setContent(callbacks = callbacks)
        composeTestRule.onNodeWithText("Sign In").performClick()
        assertTrue("onSubmit callback should have been invoked", submitInvoked)
    }

    @Test
    fun setupScreen_displaysErrorMessage() {
        setContent(uiState = SetupUiState(errorMessage = "Invalid credentials"))
        composeTestRule.onNodeWithText("Invalid credentials").assertIsDisplayed()
    }

    private data class TestCallbacks(
        val onEmailChange: (String) -> Unit = {},
        val onPasswordChange: (String) -> Unit = {},
        val onConfirmPasswordChange: (String) -> Unit = {},
        val onToggleMode: () -> Unit = {},
        val onSubmit: () -> Unit = {},
        val onClearError: () -> Unit = {}
    )
}
