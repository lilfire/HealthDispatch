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
        coEvery { authRepository.refreshAuthState() } coAnswers {
            // default: stays Unknown
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = SetupViewModel(authRepository)

    @Test
    fun `initial state is sign-in mode with empty fields`() = runTest {
        val vm = createViewModel()
        val state = vm.uiState.value
        assertFalse(state.isSignUpMode)
        assertEquals("", state.email)
        assertEquals("", state.password)
        assertEquals("", state.confirmPassword)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun `updateEmail changes email in state`() = runTest {
        val vm = createViewModel()
        vm.updateEmail("test@example.com")
        assertEquals("test@example.com", vm.uiState.value.email)
    }

    @Test
    fun `updatePassword changes password in state`() = runTest {
        val vm = createViewModel()
        vm.updatePassword("secret123")
        assertEquals("secret123", vm.uiState.value.password)
    }

    @Test
    fun `updateConfirmPassword changes confirmPassword in state`() = runTest {
        val vm = createViewModel()
        vm.updateConfirmPassword("secret123")
        assertEquals("secret123", vm.uiState.value.confirmPassword)
    }

    @Test
    fun `toggleMode switches between sign-in and sign-up`() = runTest {
        val vm = createViewModel()
        assertFalse(vm.uiState.value.isSignUpMode)
        vm.toggleMode()
        assertTrue(vm.uiState.value.isSignUpMode)
        vm.toggleMode()
        assertFalse(vm.uiState.value.isSignUpMode)
    }

    @Test
    fun `toggleMode clears error message`() = runTest {
        val vm = createViewModel()
        vm.updateEmail("bad")
        vm.submit()
        advanceUntilIdle()
        // Should have an error now
        vm.toggleMode()
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun `sign-in validates email is not blank`() = runTest {
        val vm = createViewModel()
        vm.updatePassword("password123")
        vm.submit()
        advanceUntilIdle()
        assertEquals("Please enter your email address", vm.uiState.value.errorMessage)
    }

    @Test
    fun `sign-in validates password is not blank`() = runTest {
        val vm = createViewModel()
        vm.updateEmail("test@example.com")
        vm.submit()
        advanceUntilIdle()
        assertEquals("Please enter your password", vm.uiState.value.errorMessage)
    }

    @Test
    fun `sign-in validates email format`() = runTest {
        val vm = createViewModel()
        vm.updateEmail("notanemail")
        vm.updatePassword("password123")
        vm.submit()
        advanceUntilIdle()
        assertEquals("Please enter a valid email address", vm.uiState.value.errorMessage)
    }

    @Test
    fun `sign-up validates passwords match`() = runTest {
        val vm = createViewModel()
        vm.toggleMode()
        vm.updateEmail("test@example.com")
        vm.updatePassword("password123")
        vm.updateConfirmPassword("different")
        vm.submit()
        advanceUntilIdle()
        assertEquals("Passwords do not match", vm.uiState.value.errorMessage)
    }

    @Test
    fun `sign-up validates password minimum length`() = runTest {
        val vm = createViewModel()
        vm.toggleMode()
        vm.updateEmail("test@example.com")
        vm.updatePassword("12345")
        vm.updateConfirmPassword("12345")
        vm.submit()
        advanceUntilIdle()
        assertEquals("Password must be at least 6 characters", vm.uiState.value.errorMessage)
    }

    @Test
    fun `successful sign-in calls repository and sets loading state`() = runTest {
        coEvery { authRepository.signIn(any(), any()) } returns Result.success(Unit)
        val vm = createViewModel()

        vm.uiState.test {
            awaitItem() // initial state

            vm.updateEmail("test@example.com")
            awaitItem()
            vm.updatePassword("password123")
            awaitItem()
            vm.submit()

            // Should show loading
            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)

            // Should complete with auth success event
            advanceUntilIdle()
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { authRepository.signIn("test@example.com", "password123") }
    }

    @Test
    fun `failed sign-in shows error message`() = runTest {
        coEvery { authRepository.signIn(any(), any()) } returns
            Result.failure(Exception("Invalid login credentials"))
        val vm = createViewModel()
        vm.updateEmail("test@example.com")
        vm.updatePassword("wrongpassword")
        vm.submit()
        advanceUntilIdle()
        assertEquals("Invalid login credentials", vm.uiState.value.errorMessage)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `successful sign-up calls repository`() = runTest {
        coEvery { authRepository.signUp(any(), any()) } returns Result.success(Unit)
        val vm = createViewModel()
        vm.toggleMode()
        vm.updateEmail("new@example.com")
        vm.updatePassword("password123")
        vm.updateConfirmPassword("password123")
        vm.submit()
        advanceUntilIdle()
        coVerify { authRepository.signUp("new@example.com", "password123") }
    }

    @Test
    fun `failed sign-up shows error message`() = runTest {
        coEvery { authRepository.signUp(any(), any()) } returns
            Result.failure(Exception("User already registered"))
        val vm = createViewModel()
        vm.toggleMode()
        vm.updateEmail("existing@example.com")
        vm.updatePassword("password123")
        vm.updateConfirmPassword("password123")
        vm.submit()
        advanceUntilIdle()
        assertEquals("User already registered", vm.uiState.value.errorMessage)
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
    fun `handleGoogleSignIn sets loading true while in progress`() = runTest {
        coEvery { authRepository.signInWithGoogle(any()) } coAnswers {
            kotlinx.coroutines.delay(5000)
            Result.success(Unit)
        }
        val vm = createViewModel()
        vm.handleGoogleSignIn("google-id-token-123")
        testScheduler.advanceTimeBy(100)
        assertTrue(vm.uiState.value.isLoading)
    }

    @Test
    fun `handleGoogleSignIn clears loading on success`() = runTest {
        coEvery { authRepository.signInWithGoogle(any()) } returns Result.success(Unit)
        val vm = createViewModel()
        vm.handleGoogleSignIn("google-id-token-123")
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `handleGoogleSignIn failure shows error`() = runTest {
        coEvery { authRepository.signInWithGoogle(any()) } returns
            Result.failure(Exception("Google sign-in failed"))
        val vm = createViewModel()
        vm.handleGoogleSignIn("bad-token")
        advanceUntilIdle()
        assertEquals("Google sign-in failed", vm.uiState.value.errorMessage)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `handleGoogleSignIn clears previous error`() = runTest {
        coEvery { authRepository.signInWithGoogle(any()) } returns Result.success(Unit)
        val vm = createViewModel()
        // Set an existing error first
        vm.submit() // triggers validation error
        advanceUntilIdle()
        assertTrue(vm.uiState.value.errorMessage != null)
        // Google sign-in should clear the error
        vm.handleGoogleSignIn("google-id-token-123")
        testScheduler.advanceTimeBy(100)
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun `handleGoogleSignInError sets error message`() = runTest {
        val vm = createViewModel()
        vm.handleGoogleSignInError("No Google accounts found. Please add a Google account to your device")
        assertEquals(
            "No Google accounts found. Please add a Google account to your device",
            vm.uiState.value.errorMessage
        )
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `setGoogleSignInLoading sets loading true and clears error`() = runTest {
        val vm = createViewModel()
        // Set an error first
        vm.submit()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.errorMessage != null)
        // Now set loading
        vm.setGoogleSignInLoading()
        assertTrue(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun `clearError removes error message`() = runTest {
        val vm = createViewModel()
        vm.submit() // triggers validation error
        advanceUntilIdle()
        assertTrue(vm.uiState.value.errorMessage != null)
        vm.clearError()
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun `auth state authenticated emits navigation event`() = runTest {
        authStateFlow.value = AuthState.Authenticated
        val vm = createViewModel()
        advanceUntilIdle()
        vm.authSuccessEvent.test {
            // Check if event was emitted
            val event = awaitItem()
            assertTrue(event)
        }
    }

    @Test
    fun `submit does nothing while loading`() = runTest {
        coEvery { authRepository.signIn(any(), any()) } coAnswers {
            kotlinx.coroutines.delay(5000)
            Result.success(Unit)
        }
        val vm = createViewModel()
        vm.updateEmail("test@example.com")
        vm.updatePassword("password123")
        vm.submit()
        testScheduler.advanceTimeBy(100)
        assertTrue(vm.uiState.value.isLoading)
        // Second submit should be ignored
        vm.submit()
        // Still only one call should have been made
        coVerify(exactly = 1) { authRepository.signIn(any(), any()) }
    }
}
