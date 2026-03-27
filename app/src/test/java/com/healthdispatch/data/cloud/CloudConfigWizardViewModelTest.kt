package com.healthdispatch.data.cloud

import app.cash.turbine.test
import com.healthdispatch.ui.onboarding.CloudConfigWizardViewModel
import com.healthdispatch.ui.onboarding.WizardStep
import com.healthdispatch.ui.onboarding.WizardState
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
class CloudConfigWizardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var managementApi: SupabaseManagementApi
    private lateinit var cloudConfigRepository: CloudConfigRepository
    private lateinit var viewModel: CloudConfigWizardViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        managementApi = mockk()
        cloudConfigRepository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): CloudConfigWizardViewModel {
        return CloudConfigWizardViewModel(managementApi, cloudConfigRepository)
    }

    @Test
    fun `initial state is SIGN_IN step with no error`() = runTest {
        viewModel = createViewModel()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(WizardStep.SIGN_IN, state.step)
            assertNull(state.error)
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `startWizard with access token fetches projects and orgs`() = runTest {
        coEvery { managementApi.listProjects(any()) } returns Result.success(
            listOf(SupabaseProject("proj-1", "My Project", "org-1", "us-east-1", status = "ACTIVE_HEALTHY"))
        )
        coEvery { managementApi.getOrganizations(any()) } returns Result.success(
            listOf(SupabaseOrganization("org-1", "My Org"))
        )

        viewModel = createViewModel()
        viewModel.onAccessTokenReceived("test-token")
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(WizardStep.SELECT_PROJECT, state.step)
            assertEquals(1, state.projects.size)
            assertEquals("proj-1", state.projects[0].id)
            assertEquals(1, state.organizations.size)
        }
    }

    @Test
    fun `startWizard with no existing projects goes to CREATE_PROJECT`() = runTest {
        coEvery { managementApi.listProjects(any()) } returns Result.success(emptyList())
        coEvery { managementApi.getOrganizations(any()) } returns Result.success(
            listOf(SupabaseOrganization("org-1", "My Org"))
        )

        viewModel = createViewModel()
        viewModel.onAccessTokenReceived("test-token")
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(WizardStep.CREATE_PROJECT, state.step)
        }
    }

    @Test
    fun `selectProject fetches API keys and saves config`() = runTest {
        val project = SupabaseProject("proj-1", "Test", "org-1", "us-east-1", status = "ACTIVE_HEALTHY")
        coEvery { managementApi.listProjects(any()) } returns Result.success(listOf(project))
        coEvery { managementApi.getOrganizations(any()) } returns Result.success(
            listOf(SupabaseOrganization("org-1", "My Org"))
        )
        coEvery { managementApi.getApiKeys(any(), "proj-1") } returns Result.success(
            listOf(
                SupabaseApiKey("anon", "anon-key-123"),
                SupabaseApiKey("service_role", "service-key-456")
            )
        )
        coEvery { managementApi.runSql(any(), any(), any()) } returns Result.success(Unit)

        viewModel = createViewModel()
        viewModel.onAccessTokenReceived("test-token")
        advanceUntilIdle()

        viewModel.selectProject(project)
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(WizardStep.COMPLETE, state.step)
        }

        coVerify { cloudConfigRepository.saveCloudConfig("https://proj-1.supabase.co", "anon-key-123") }
    }

    @Test
    fun `createNewProject creates project, runs migrations, and saves config`() = runTest {
        coEvery { managementApi.listProjects(any()) } returns Result.success(emptyList())
        coEvery { managementApi.getOrganizations(any()) } returns Result.success(
            listOf(SupabaseOrganization("org-1", "My Org"))
        )
        coEvery { managementApi.createProject(any(), any(), "org-1", any(), any()) } returns Result.success(
            SupabaseProject("new-proj", "HealthDispatch", "org-1", "us-east-1", status = "COMING_UP")
        )
        coEvery { managementApi.getApiKeys(any(), "new-proj") } returns Result.success(
            listOf(SupabaseApiKey("anon", "new-anon-key"))
        )
        coEvery { managementApi.runSql(any(), any(), any()) } returns Result.success(Unit)

        viewModel = createViewModel()
        viewModel.onAccessTokenReceived("test-token")
        advanceUntilIdle()

        viewModel.createNewProject("org-1")
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(WizardStep.COMPLETE, state.step)
        }

        coVerify { managementApi.createProject(any(), any(), "org-1", any(), "us-east-1") }
        coVerify { managementApi.runSql(any(), "new-proj", any()) }
        coVerify { cloudConfigRepository.saveCloudConfig("https://new-proj.supabase.co", "new-anon-key") }
    }

    @Test
    fun `createNewProject shows error when API fails`() = runTest {
        coEvery { managementApi.listProjects(any()) } returns Result.success(emptyList())
        coEvery { managementApi.getOrganizations(any()) } returns Result.success(
            listOf(SupabaseOrganization("org-1", "My Org"))
        )
        coEvery { managementApi.createProject(any(), any(), any(), any(), any()) } returns
            Result.failure(Exception("Project limit reached"))

        viewModel = createViewModel()
        viewModel.onAccessTokenReceived("test-token")
        advanceUntilIdle()

        viewModel.createNewProject("org-1")
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(WizardStep.CREATE_PROJECT, state.step)
            assertTrue(state.error?.contains("Project limit reached") == true)
        }
    }

    @Test
    fun `selectProject shows error when getApiKeys fails`() = runTest {
        val project = SupabaseProject("proj-1", "Test", "org-1", "us-east-1", status = "ACTIVE_HEALTHY")
        coEvery { managementApi.listProjects(any()) } returns Result.success(listOf(project))
        coEvery { managementApi.getOrganizations(any()) } returns Result.success(
            listOf(SupabaseOrganization("org-1", "My Org"))
        )
        coEvery { managementApi.getApiKeys(any(), "proj-1") } returns
            Result.failure(Exception("Unauthorized"))

        viewModel = createViewModel()
        viewModel.onAccessTokenReceived("test-token")
        advanceUntilIdle()

        viewModel.selectProject(project)
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.error?.contains("Unauthorized") == true)
        }
    }

    @Test
    fun `onAccessTokenReceived shows error when listProjects fails`() = runTest {
        coEvery { managementApi.listProjects(any()) } returns
            Result.failure(Exception("Network error"))
        coEvery { managementApi.getOrganizations(any()) } returns Result.success(emptyList())

        viewModel = createViewModel()
        viewModel.onAccessTokenReceived("test-token")
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertTrue(state.error?.contains("Network error") == true)
        }
    }

    @Test
    fun `clearError resets error in state`() = runTest {
        coEvery { managementApi.listProjects(any()) } returns
            Result.failure(Exception("Error"))
        coEvery { managementApi.getOrganizations(any()) } returns Result.success(emptyList())

        viewModel = createViewModel()
        viewModel.onAccessTokenReceived("test-token")
        advanceUntilIdle()

        viewModel.clearError()
        advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertNull(state.error)
        }
    }

    @Test
    fun `loading state is true while fetching projects`() = runTest {
        coEvery { managementApi.listProjects(any()) } returns Result.success(emptyList())
        coEvery { managementApi.getOrganizations(any()) } returns Result.success(emptyList())

        viewModel = createViewModel()

        viewModel.state.test {
            val initial = awaitItem()
            assertFalse(initial.isLoading)

            viewModel.onAccessTokenReceived("test-token")
            val loading = awaitItem()
            assertTrue(loading.isLoading)

            advanceUntilIdle()
            val done = awaitItem()
            assertFalse(done.isLoading)
        }
    }

    @Test
    fun `goBackToProjectList returns to SELECT_PROJECT from CREATE_PROJECT`() = runTest {
        coEvery { managementApi.listProjects(any()) } returns Result.success(
            listOf(SupabaseProject("proj-1", "Test", "org-1", "us-east-1"))
        )
        coEvery { managementApi.getOrganizations(any()) } returns Result.success(
            listOf(SupabaseOrganization("org-1", "My Org"))
        )

        viewModel = createViewModel()
        viewModel.onAccessTokenReceived("test-token")
        advanceUntilIdle()

        viewModel.goToCreateProject()
        advanceUntilIdle()

        viewModel.state.test {
            assertEquals(WizardStep.CREATE_PROJECT, awaitItem().step)
        }

        viewModel.goBackToProjectList()
        advanceUntilIdle()

        viewModel.state.test {
            assertEquals(WizardStep.SELECT_PROJECT, awaitItem().step)
        }
    }
}
