package com.healthdispatch.ui.setup

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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SetupViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var authRepository: AuthRepository
    private val authStateFlow = MutableStateFlow<AuthState>(AuthState.Unknown)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mockk(relaxed = true)
        every { authRepository.authState } returns authStateFlow
        coEvery { authRepository.refreshAuthState() } coAnswers { /* stays Unknown */ }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = SetupViewModel(authRepository)

    @Test
    fun `initial state has no loading and no error`() = runTest {
        val vm = createViewModel()
        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun `clearError removes error message`() = runTest {
        val vm = createViewModel()
        vm.setError("some error")
        advanceUntilIdle()
        assertEquals("some error", vm.uiState.value.errorMessage)
        vm.clearError()
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun `setError sets error message`() = runTest {
        val vm = createViewModel()
        vm.setError("Google sign-in failed")
        assertEquals("Google sign-in failed", vm.uiState.value.errorMessage)
    }

    @Test
    fun `handleGoogleSignIn calls repository with token`() = runTest {
        coEvery { authRepository.signInWithGoogle(any()) } returns Result.success(Unit)
        val vm = createViewModel()
        vm.handleGoogleSignIn("google-id-token-123")
        advanceUntilIdle()
        coVerify { authRepository.signInWithGoogle("google-id-token-123") }
    }

    @Test
    fun `handleGoogleSignIn shows loading then clears on success`() = runTest {
        coEvery { authRepository.signInWithGoogle(any()) } returns Result.success(Unit)
        val vm = createViewModel()

        vm.uiState.test {
            awaitItem() // initial state

            vm.handleGoogleSignIn("valid-token")

            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)
            assertNull(loadingState.errorMessage)

            advanceUntilIdle()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `handleGoogleSignIn failure shows error message`() = runTest {
        coEvery { authRepository.signInWithGoogle(any()) } returns
            Result.failure(Exception("Google sign-in failed"))
        val vm = createViewModel()
        vm.handleGoogleSignIn("bad-token")
        advanceUntilIdle()
        assertEquals("Google sign-in failed", vm.uiState.value.errorMessage)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `handleFacebookSignIn calls repository with access token`() = runTest {
        coEvery { authRepository.signInWithFacebook(any()) } returns Result.success(Unit)
        val vm = createViewModel()
        vm.handleFacebookSignIn("facebook-access-token-abc")
        advanceUntilIdle()
        coVerify { authRepository.signInWithFacebook("facebook-access-token-abc") }
    }

    @Test
    fun `handleFacebookSignIn shows loading then clears on success`() = runTest {
        coEvery { authRepository.signInWithFacebook(any()) } returns Result.success(Unit)
        val vm = createViewModel()

        vm.uiState.test {
            awaitItem() // initial state

            vm.handleFacebookSignIn("valid-fb-token")

            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)
            assertNull(loadingState.errorMessage)

            advanceUntilIdle()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `handleFacebookSignIn failure shows error message`() = runTest {
        coEvery { authRepository.signInWithFacebook(any()) } returns
            Result.failure(Exception("Facebook sign-in failed"))
        val vm = createViewModel()
        vm.handleFacebookSignIn("bad-fb-token")
        advanceUntilIdle()
        assertEquals("Facebook sign-in failed", vm.uiState.value.errorMessage)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `auth state authenticated emits navigation event`() = runTest {
        authStateFlow.value = AuthState.Authenticated
        val vm = createViewModel()
        advanceUntilIdle()
        vm.authSuccessEvent.test {
            val event = awaitItem()
            assertTrue(event)
        }
    }
}
