package com.healthdispatch.ui.navigation

import app.cash.turbine.test
import com.healthdispatch.data.auth.AuthRepository
import com.healthdispatch.data.auth.AuthState
import com.healthdispatch.data.cloud.CloudConfigRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NavViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var authRepository: AuthRepository
    private lateinit var cloudConfigRepository: CloudConfigRepository
    private val authStateFlow = MutableStateFlow<AuthState>(AuthState.Unknown)
    private val onboardingCompleteFlow = MutableStateFlow(true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mockk(relaxed = true)
        cloudConfigRepository = mockk(relaxed = true)
        every { authRepository.authState } returns authStateFlow
        every { cloudConfigRepository.onboardingCompleteFlow } returns onboardingCompleteFlow
        coEvery { authRepository.refreshAuthState() } coAnswers {}
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = NavViewModel(authRepository, cloudConfigRepository)

    @Test
    fun `initial auth state is Unknown`() = runTest {
        val vm = createViewModel()
        assertEquals(AuthState.Unknown, vm.authState.value)
    }

    @Test
    fun `auth state transitions to Authenticated`() = runTest {
        val vm = createViewModel()
        vm.authState.test {
            assertEquals(AuthState.Unknown, awaitItem())

            authStateFlow.value = AuthState.Authenticated
            assertEquals(AuthState.Authenticated, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `auth state transitions to Unauthenticated`() = runTest {
        val vm = createViewModel()
        vm.authState.test {
            assertEquals(AuthState.Unknown, awaitItem())

            authStateFlow.value = AuthState.Unauthenticated
            assertEquals(AuthState.Unauthenticated, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refreshAuthState is called on init`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()
        coVerify { authRepository.refreshAuthState() }
    }

    @Test
    fun `auth state reflects full lifecycle`() = runTest {
        val vm = createViewModel()
        vm.authState.test {
            assertEquals(AuthState.Unknown, awaitItem())

            authStateFlow.value = AuthState.Unauthenticated
            assertEquals(AuthState.Unauthenticated, awaitItem())

            authStateFlow.value = AuthState.Authenticated
            assertEquals(AuthState.Authenticated, awaitItem())

            authStateFlow.value = AuthState.Unauthenticated
            assertEquals(AuthState.Unauthenticated, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onboardingComplete defaults to true`() = runTest {
        val vm = createViewModel()
        assertTrue(vm.onboardingComplete.value)
    }

    @Test
    fun `onboardingComplete reflects cloud config state`() = runTest {
        onboardingCompleteFlow.value = false
        val vm = createViewModel()
        vm.onboardingComplete.test {
            assertFalse(awaitItem())

            onboardingCompleteFlow.value = true
            assertTrue(awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }
}
