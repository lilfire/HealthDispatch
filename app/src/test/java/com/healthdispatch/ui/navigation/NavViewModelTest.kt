package com.healthdispatch.ui.navigation

import app.cash.turbine.test
import com.healthdispatch.data.auth.AuthRepository
import com.healthdispatch.data.auth.AuthState
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NavViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var authRepository: AuthRepository
    private val authStateFlow = MutableStateFlow<AuthState>(AuthState.Unknown)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mockk(relaxed = true)
        every { authRepository.authState } returns authStateFlow
        coEvery { authRepository.refreshAuthState() } coAnswers {}
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = NavViewModel(authRepository)

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
}
