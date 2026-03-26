package com.healthdispatch.ui.permission

import com.healthdispatch.data.healthconnect.HealthConnectRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HealthPermissionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var healthConnectRepository: HealthConnectRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        healthConnectRepository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = HealthPermissionViewModel(healthConnectRepository)

    @Test
    fun `initial state shows available and not granted`() = runTest {
        coEvery { healthConnectRepository.isAvailable() } returns true
        coEvery { healthConnectRepository.hasAllPermissions() } returns false

        val vm = createViewModel()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.healthConnectAvailable)
        assertFalse(vm.uiState.value.allGranted)
    }

    @Test
    fun `marks not available when Health Connect is unavailable`() = runTest {
        coEvery { healthConnectRepository.isAvailable() } returns false

        val vm = createViewModel()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.healthConnectAvailable)
        assertFalse(vm.uiState.value.allGranted)
    }

    @Test
    fun `marks all granted when permissions already exist`() = runTest {
        coEvery { healthConnectRepository.isAvailable() } returns true
        coEvery { healthConnectRepository.hasAllPermissions() } returns true

        val vm = createViewModel()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.allGranted)
    }

    @Test
    fun `onPermissionResult updates granted state`() = runTest {
        coEvery { healthConnectRepository.isAvailable() } returns true
        coEvery { healthConnectRepository.hasAllPermissions() } returnsMany listOf(false, true)

        val vm = createViewModel()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.allGranted)

        var granted = false
        vm.onPermissionResult(onGranted = { granted = true })
        advanceUntilIdle()

        assertTrue(vm.uiState.value.allGranted)
        assertTrue(granted)
    }

    @Test
    fun `onPermissionResult does not call onGranted when not all permissions granted`() = runTest {
        coEvery { healthConnectRepository.isAvailable() } returns true
        coEvery { healthConnectRepository.hasAllPermissions() } returns false

        val vm = createViewModel()
        advanceUntilIdle()

        var granted = false
        vm.onPermissionResult(onGranted = { granted = true })
        advanceUntilIdle()

        assertFalse(granted)
    }

    @Test
    fun `handles exception from isAvailable gracefully`() = runTest {
        coEvery { healthConnectRepository.isAvailable() } throws RuntimeException("not installed")

        val vm = createViewModel()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.healthConnectAvailable)
    }

    @Test
    fun `handles exception from hasAllPermissions gracefully`() = runTest {
        coEvery { healthConnectRepository.isAvailable() } returns true
        coEvery { healthConnectRepository.hasAllPermissions() } throws RuntimeException("error")

        val vm = createViewModel()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.healthConnectAvailable)
        assertFalse(vm.uiState.value.allGranted)
    }

    @Test
    fun `requiredPermissions delegates to repository`() = runTest {
        val perms = setOf("perm1", "perm2")
        coEvery { healthConnectRepository.requiredPermissions } returns perms
        coEvery { healthConnectRepository.isAvailable() } returns true
        coEvery { healthConnectRepository.hasAllPermissions() } returns false

        val vm = createViewModel()
        assertTrue(vm.requiredPermissions == perms)
    }
}
