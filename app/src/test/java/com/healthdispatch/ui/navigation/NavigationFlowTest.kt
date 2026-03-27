package com.healthdispatch.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NavigationFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun setupIsStartDestination() {
        composeTestRule.setContent {
            HealthDispatchNavHost()
        }
        composeTestRule.onNodeWithText("HealthDispatch").assertIsDisplayed()
        composeTestRule.onNode(hasText("Supabase URL") and hasSetTextAction())
            .assertIsDisplayed()
    }

    @Test
    fun setupCompletion_navigatesToDashboard() {
        composeTestRule.setContent {
            HealthDispatchNavHost()
        }
        composeTestRule.onNode(hasText("Supabase URL") and hasSetTextAction())
            .performTextInput("https://test.supabase.co")
        composeTestRule.onNode(hasText("Supabase API Key") and hasSetTextAction())
            .performTextInput("test-api-key")
        composeTestRule.onNodeWithText("Connect & Start Syncing").performClick()

        composeTestRule.onNodeWithText("Sync Status").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pending Records").assertIsDisplayed()
    }

    @Test
    fun dashboard_navigateToSettings() {
        composeTestRule.setContent {
            HealthDispatchNavHost()
        }
        // Navigate to dashboard
        composeTestRule.onNode(hasText("Supabase URL") and hasSetTextAction())
            .performTextInput("https://test.supabase.co")
        composeTestRule.onNode(hasText("Supabase API Key") and hasSetTextAction())
            .performTextInput("test-api-key")
        composeTestRule.onNodeWithText("Connect & Start Syncing").performClick()

        // Navigate to settings
        composeTestRule.onNodeWithContentDescription("Settings").performClick()

        composeTestRule.onNodeWithText("Supabase URL").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sync Interval").assertIsDisplayed()
        composeTestRule.onNodeWithText("Data Types").assertIsDisplayed()
    }

    @Test
    fun settings_backNavigationReturnsToDashboard() {
        composeTestRule.setContent {
            HealthDispatchNavHost()
        }
        // Navigate to dashboard
        composeTestRule.onNode(hasText("Supabase URL") and hasSetTextAction())
            .performTextInput("https://test.supabase.co")
        composeTestRule.onNode(hasText("Supabase API Key") and hasSetTextAction())
            .performTextInput("test-api-key")
        composeTestRule.onNodeWithText("Connect & Start Syncing").performClick()

        // Navigate to settings
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.onNodeWithText("Sync Interval").assertIsDisplayed()

        // Navigate back
        composeTestRule.onNodeWithContentDescription("Back").performClick()

        // Should be back on dashboard
        composeTestRule.onNodeWithText("Sync Status").assertIsDisplayed()
    }
}
