package com.healthdispatch.ui.setup

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SetupScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun setupScreen_displaysBothInputFields() {
        composeTestRule.setContent {
            SetupScreen(onSetupComplete = {})
        }
        composeTestRule.onNode(hasText("Supabase URL") and hasSetTextAction())
            .assertIsDisplayed()
        composeTestRule.onNode(hasText("Supabase API Key") and hasSetTextAction())
            .assertIsDisplayed()
    }

    @Test
    fun setupScreen_displaysTitle() {
        composeTestRule.setContent {
            SetupScreen(onSetupComplete = {})
        }
        composeTestRule.onNodeWithText("HealthDispatch").assertIsDisplayed()
        composeTestRule.onNodeWithText("Configure your cloud endpoint").assertIsDisplayed()
    }

    @Test
    fun setupScreen_buttonDisabledWhenFieldsBlank() {
        composeTestRule.setContent {
            SetupScreen(onSetupComplete = {})
        }
        composeTestRule.onNodeWithText("Connect & Start Syncing").assertIsNotEnabled()
    }

    @Test
    fun setupScreen_buttonDisabledWhenOnlyUrlFilled() {
        composeTestRule.setContent {
            SetupScreen(onSetupComplete = {})
        }
        composeTestRule.onNode(hasText("Supabase URL") and hasSetTextAction())
            .performTextInput("https://test.supabase.co")
        composeTestRule.onNodeWithText("Connect & Start Syncing").assertIsNotEnabled()
    }

    @Test
    fun setupScreen_buttonDisabledWhenOnlyKeyFilled() {
        composeTestRule.setContent {
            SetupScreen(onSetupComplete = {})
        }
        composeTestRule.onNode(hasText("Supabase API Key") and hasSetTextAction())
            .performTextInput("test-api-key")
        composeTestRule.onNodeWithText("Connect & Start Syncing").assertIsNotEnabled()
    }

    @Test
    fun setupScreen_buttonEnabledWhenBothFieldsFilled() {
        composeTestRule.setContent {
            SetupScreen(onSetupComplete = {})
        }
        composeTestRule.onNode(hasText("Supabase URL") and hasSetTextAction())
            .performTextInput("https://test.supabase.co")
        composeTestRule.onNode(hasText("Supabase API Key") and hasSetTextAction())
            .performTextInput("test-api-key")
        composeTestRule.onNodeWithText("Connect & Start Syncing").assertIsEnabled()
    }

    @Test
    fun setupScreen_buttonClickTriggersCallback() {
        var callbackInvoked = false
        composeTestRule.setContent {
            SetupScreen(onSetupComplete = { callbackInvoked = true })
        }
        composeTestRule.onNode(hasText("Supabase URL") and hasSetTextAction())
            .performTextInput("https://test.supabase.co")
        composeTestRule.onNode(hasText("Supabase API Key") and hasSetTextAction())
            .performTextInput("test-api-key")
        composeTestRule.onNodeWithText("Connect & Start Syncing").performClick()
        assertTrue("onSetupComplete callback should have been invoked", callbackInvoked)
    }

    @Test
    fun setupScreen_buttonDisabledWhenFieldsContainOnlySpaces() {
        composeTestRule.setContent {
            SetupScreen(onSetupComplete = {})
        }
        composeTestRule.onNode(hasText("Supabase URL") and hasSetTextAction())
            .performTextInput("   ")
        composeTestRule.onNode(hasText("Supabase API Key") and hasSetTextAction())
            .performTextInput("   ")
        composeTestRule.onNodeWithText("Connect & Start Syncing").assertIsNotEnabled()
    }
}
