package com.healthdispatch.ui.settings

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
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var authRepository: AuthRepository
    private val authStateFlow = MutableStateFlow<AuthState>(AuthState.Authenticated)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mockk(relaxed = true)
        every { authRepository.authState } returns authStateFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = SettingsViewModel(authRepository)

    @Test
    fun `initial state has no loading and no error`() = runTest {
        val vm = createViewModel()
        assertFalse(vm.uiState.value.isSigningOut)
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun `signOut calls repository`() = runTest {
        coEvery { authRepository.signOut() } returns Result.success(Unit)
        val vm = createViewModel()
        vm.signOut()
        advanceUntilIdle()
        coVerify { authRepository.signOut() }
    }

    @Test
    fun `signOut success emits signOutEvent`() = runTest {
        coEvery { authRepository.signOut() } returns Result.success(Unit)
        val vm = createViewModel()

        vm.signOutEvent.test {
            vm.signOut()
            advanceUntilIdle()
            val event = awaitItem()
            assertTrue(event)
        }
    }

    @Test
    fun `signOut failure shows error message`() = runTest {
        coEvery { authRepository.signOut() } returns
            Result.failure(Exception("Session expired"))
        val vm = createViewModel()
        vm.signOut()
        advanceUntilIdle()
        assertEquals("Session expired", vm.uiState.value.errorMessage)
        assertFalse(vm.uiState.value.isSigningOut)
    }

    @Test
    fun `signOut sets loading state`() = runTest {
        coEvery { authRepository.signOut() } coAnswers {
            kotlinx.coroutines.delay(5000)
            Result.success(Unit)
        }
        val vm = createViewModel()

        vm.uiState.test {
            awaitItem() // initial

            vm.signOut()
            val loading = awaitItem()
            assertTrue(loading.isSigningOut)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `signOut ignores duplicate calls while loading`() = runTest {
        coEvery { authRepository.signOut() } coAnswers {
            kotlinx.coroutines.delay(5000)
            Result.success(Unit)
        }
        val vm = createViewModel()
        vm.signOut()
        testScheduler.advanceTimeBy(100)
        assertTrue(vm.uiState.value.isSigningOut)
        vm.signOut() // should be ignored
        coVerify(exactly = 1) { authRepository.signOut() }
    }
}
