package com.healthdispatch.ui.setup

import com.healthdispatch.data.auth.AuthClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
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
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SetupViewModelTest {

    private lateinit var mockAuthClient: AuthClient
    private lateinit var viewModel: SetupViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockAuthClient = mockk()
        viewModel = SetupViewModel(mockAuthClient)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has empty fields and no error`() {
        val state = viewModel.uiState.value
        assertEquals("", state.email)
        assertEquals("", state.password)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertFalse(state.isAuthenticated)
    }

    @Test
    fun `onEmailChange updates email in state`() {
        viewModel.onEmailChange("user@example.com")
        assertEquals("user@example.com", viewModel.uiState.value.email)
    }

    @Test
    fun `onPasswordChange updates password in state`() {
        viewModel.onPasswordChange("secret123")
        assertEquals("secret123", viewModel.uiState.value.password)
    }

    @Test
    fun `signIn sets isLoading true then false on success`() = runTest(testDispatcher) {
        coEvery { mockAuthClient.signInWithEmail(any(), any()) } returns Result.success(Unit)

        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("password123")
        viewModel.signIn()

        // After launching but before completion, loading should be true
        assertTrue(viewModel.uiState.value.isLoading)

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.isAuthenticated)
    }

    @Test
    fun `signIn calls authClient with correct email and password`() = runTest(testDispatcher) {
        coEvery { mockAuthClient.signInWithEmail(any(), any()) } returns Result.success(Unit)

        viewModel.onEmailChange("test@example.com")
        viewModel.onPasswordChange("mypassword")
        viewModel.signIn()
        advanceUntilIdle()

        coVerify(exactly = 1) { mockAuthClient.signInWithEmail("test@example.com", "mypassword") }
    }

    @Test
    fun `signIn sets error message on failure`() = runTest(testDispatcher) {
        coEvery { mockAuthClient.signInWithEmail(any(), any()) } returns
                Result.failure(Exception("Invalid credentials"))

        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("wrong")
        viewModel.signIn()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.isAuthenticated)
        assertEquals("Invalid credentials", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `signIn does nothing when email is blank`() = runTest(testDispatcher) {
        viewModel.onPasswordChange("password123")
        viewModel.signIn()
        advanceUntilIdle()

        coVerify(exactly = 0) { mockAuthClient.signInWithEmail(any(), any()) }
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `signIn does nothing when password is blank`() = runTest(testDispatcher) {
        viewModel.onEmailChange("user@example.com")
        viewModel.signIn()
        advanceUntilIdle()

        coVerify(exactly = 0) { mockAuthClient.signInWithEmail(any(), any()) }
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `signIn clears previous error before attempting`() = runTest(testDispatcher) {
        coEvery { mockAuthClient.signInWithEmail(any(), any()) } returns
                Result.failure(Exception("First error"))

        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("pass")
        viewModel.signIn()
        advanceUntilIdle()

        assertEquals("First error", viewModel.uiState.value.errorMessage)

        coEvery { mockAuthClient.signInWithEmail(any(), any()) } returns Result.success(Unit)
        viewModel.signIn()

        // Error should be cleared immediately when new sign-in starts
        assertNull(viewModel.uiState.value.errorMessage)

        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isAuthenticated)
    }

    @Test
    fun `signIn prevents concurrent requests`() = runTest(testDispatcher) {
        coEvery { mockAuthClient.signInWithEmail(any(), any()) } returns Result.success(Unit)

        viewModel.onEmailChange("user@example.com")
        viewModel.onPasswordChange("pass")
        viewModel.signIn()
        viewModel.signIn() // second call while first is loading

        advanceUntilIdle()

        coVerify(exactly = 1) { mockAuthClient.signInWithEmail(any(), any()) }
    }
}
