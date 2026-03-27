package com.healthdispatch.ui.onboarding

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var viewModel: OnboardingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        dataStore = PreferenceDataStoreFactory.create {
            File(tmpFolder.newFolder(), "test_prefs.preferences_pb")
        }
        viewModel = OnboardingViewModel(dataStore)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state starts at WELCOME step`() = runTest {
        viewModel.state.test {
            val state = awaitItem()
            assertEquals(OnboardingStep.WELCOME, state.currentStep)
            assertNull(state.pathChoice)
            assertFalse(state.permissionsGranted)
            assertFalse(state.isComplete)
        }
    }

    @Test
    fun `goToNext from WELCOME goes to PATH_CHOICE`() = runTest {
        viewModel.goToNext()
        viewModel.state.test {
            assertEquals(OnboardingStep.PATH_CHOICE, awaitItem().currentStep)
        }
    }

    @Test
    fun `goToNext from PATH_CHOICE with SKIP goes to PERMISSIONS`() = runTest {
        viewModel.goToNext() // WELCOME -> PATH_CHOICE
        viewModel.setPathChoice(PathChoice.SKIP)
        viewModel.goToNext() // PATH_CHOICE -> PERMISSIONS

        viewModel.state.test {
            assertEquals(OnboardingStep.PERMISSIONS, awaitItem().currentStep)
        }
    }

    @Test
    fun `goToNext from PATH_CHOICE with SETUP_NEW goes to CLOUD_CONFIG`() = runTest {
        viewModel.goToNext() // WELCOME -> PATH_CHOICE
        viewModel.setPathChoice(PathChoice.SETUP_NEW)
        viewModel.goToNext() // PATH_CHOICE -> CLOUD_CONFIG

        viewModel.state.test {
            assertEquals(OnboardingStep.CLOUD_CONFIG, awaitItem().currentStep)
        }
    }

    @Test
    fun `goToNext from PATH_CHOICE with CONNECT_EXISTING goes to CLOUD_CONFIG`() = runTest {
        viewModel.goToNext() // WELCOME -> PATH_CHOICE
        viewModel.setPathChoice(PathChoice.CONNECT_EXISTING)
        viewModel.goToNext() // PATH_CHOICE -> CLOUD_CONFIG

        viewModel.state.test {
            assertEquals(OnboardingStep.CLOUD_CONFIG, awaitItem().currentStep)
        }
    }

    @Test
    fun `goToNext from CLOUD_CONFIG goes to PERMISSIONS`() = runTest {
        viewModel.goToNext() // WELCOME -> PATH_CHOICE
        viewModel.setPathChoice(PathChoice.SETUP_NEW)
        viewModel.goToNext() // PATH_CHOICE -> CLOUD_CONFIG
        viewModel.goToNext() // CLOUD_CONFIG -> PERMISSIONS

        viewModel.state.test {
            assertEquals(OnboardingStep.PERMISSIONS, awaitItem().currentStep)
        }
    }

    @Test
    fun `goToNext from PERMISSIONS goes to DONE`() = runTest {
        viewModel.goToNext() // WELCOME -> PATH_CHOICE
        viewModel.setPathChoice(PathChoice.SKIP)
        viewModel.goToNext() // PATH_CHOICE -> PERMISSIONS
        viewModel.goToNext() // PERMISSIONS -> DONE

        viewModel.state.test {
            assertEquals(OnboardingStep.DONE, awaitItem().currentStep)
        }
    }

    @Test
    fun `goBack from PATH_CHOICE goes to WELCOME`() = runTest {
        viewModel.goToNext() // WELCOME -> PATH_CHOICE
        viewModel.goBack()

        viewModel.state.test {
            assertEquals(OnboardingStep.WELCOME, awaitItem().currentStep)
        }
    }

    @Test
    fun `goBack from CLOUD_CONFIG goes to PATH_CHOICE`() = runTest {
        viewModel.goToNext()
        viewModel.setPathChoice(PathChoice.SETUP_NEW)
        viewModel.goToNext()
        viewModel.goBack()

        viewModel.state.test {
            assertEquals(OnboardingStep.PATH_CHOICE, awaitItem().currentStep)
        }
    }

    @Test
    fun `goBack from PERMISSIONS with SKIP goes to PATH_CHOICE`() = runTest {
        viewModel.goToNext()
        viewModel.setPathChoice(PathChoice.SKIP)
        viewModel.goToNext() // -> PERMISSIONS
        viewModel.goBack()

        viewModel.state.test {
            assertEquals(OnboardingStep.PATH_CHOICE, awaitItem().currentStep)
        }
    }

    @Test
    fun `goBack from PERMISSIONS with non-skip goes to CLOUD_CONFIG`() = runTest {
        viewModel.goToNext()
        viewModel.setPathChoice(PathChoice.SETUP_NEW)
        viewModel.goToNext() // -> CLOUD_CONFIG
        viewModel.goToNext() // -> PERMISSIONS
        viewModel.goBack()

        viewModel.state.test {
            assertEquals(OnboardingStep.CLOUD_CONFIG, awaitItem().currentStep)
        }
    }

    @Test
    fun `setPathChoice updates state`() = runTest {
        viewModel.setPathChoice(PathChoice.CONNECT_EXISTING)
        viewModel.state.test {
            assertEquals(PathChoice.CONNECT_EXISTING, awaitItem().pathChoice)
        }
    }

    @Test
    fun `setPermissionsGranted updates state`() = runTest {
        viewModel.setPermissionsGranted(true)
        viewModel.state.test {
            assertTrue(awaitItem().permissionsGranted)
        }
    }

    @Test
    fun `completeOnboarding sets isComplete true and persists to DataStore`() = runTest {
        viewModel.completeOnboarding()
        advanceUntilIdle()

        viewModel.state.test {
            assertTrue(awaitItem().isComplete)
        }
    }

    @Test
    fun `stepIndex returns correct ordinal`() {
        assertEquals(0, viewModel.stepIndex) // WELCOME = 0
    }

    @Test
    fun `totalSteps returns correct count`() {
        assertEquals(5, viewModel.totalSteps)
    }

    @Test
    fun `goToNext from DONE is a no-op`() = runTest {
        // Navigate to DONE
        viewModel.goToNext()
        viewModel.setPathChoice(PathChoice.SKIP)
        viewModel.goToNext()
        viewModel.goToNext()
        viewModel.goToNext() // at DONE

        viewModel.goToNext() // should be no-op

        viewModel.state.test {
            assertEquals(OnboardingStep.DONE, awaitItem().currentStep)
        }
    }

    @Test
    fun `goBack from WELCOME is a no-op`() = runTest {
        viewModel.goBack() // should be no-op

        viewModel.state.test {
            assertEquals(OnboardingStep.WELCOME, awaitItem().currentStep)
        }
    }
}
