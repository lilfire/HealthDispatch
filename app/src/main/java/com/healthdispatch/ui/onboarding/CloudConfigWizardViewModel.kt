package com.healthdispatch.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthdispatch.data.cloud.CloudConfigRepository
import com.healthdispatch.data.cloud.DatabaseMigrations
import com.healthdispatch.data.cloud.SupabaseManagementApi
import com.healthdispatch.data.cloud.SupabaseOrganization
import com.healthdispatch.data.cloud.SupabaseProject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

enum class WizardStep {
    SIGN_IN,
    SELECT_PROJECT,
    CREATE_PROJECT,
    SETTING_UP,
    COMPLETE
}

data class WizardState(
    val step: WizardStep = WizardStep.SIGN_IN,
    val isLoading: Boolean = false,
    val error: String? = null,
    val projects: List<SupabaseProject> = emptyList(),
    val organizations: List<SupabaseOrganization> = emptyList(),
    val accessToken: String = ""
)

@HiltViewModel
class CloudConfigWizardViewModel @Inject constructor(
    private val managementApi: SupabaseManagementApi,
    private val cloudConfigRepository: CloudConfigRepository
) : ViewModel() {

    private val _state = MutableStateFlow(WizardState())
    val state: StateFlow<WizardState> = _state.asStateFlow()

    fun onAccessTokenReceived(accessToken: String) {
        _state.update { it.copy(accessToken = accessToken, isLoading = true, error = null) }
        viewModelScope.launch {
            val projectsResult = managementApi.listProjects(accessToken)
            val orgsResult = managementApi.getOrganizations(accessToken)

            projectsResult.fold(
                onSuccess = { projects ->
                    val orgs = orgsResult.getOrDefault(emptyList())
                    val nextStep = if (projects.isEmpty()) WizardStep.CREATE_PROJECT else WizardStep.SELECT_PROJECT
                    _state.update {
                        it.copy(
                            step = nextStep,
                            projects = projects,
                            organizations = orgs,
                            isLoading = false
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to fetch projects"
                        )
                    }
                }
            )
        }
    }

    fun selectProject(project: SupabaseProject) {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val token = _state.value.accessToken
            val keysResult = managementApi.getApiKeys(token, project.id)

            keysResult.fold(
                onSuccess = { keys ->
                    val anonKey = keys.firstOrNull { it.name == "anon" }
                    if (anonKey != null) {
                        val projectUrl = "https://${project.id}.supabase.co"

                        // Run database migrations
                        managementApi.runSql(token, project.id, DatabaseMigrations.migrationSql)

                        cloudConfigRepository.saveCloudConfig(projectUrl, anonKey.apiKey)
                        _state.update { it.copy(step = WizardStep.COMPLETE, isLoading = false) }
                    } else {
                        _state.update {
                            it.copy(isLoading = false, error = "No anon key found for project")
                        }
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(isLoading = false, error = error.message ?: "Failed to get API keys")
                    }
                }
            )
        }
    }

    fun createNewProject(organizationId: String) {
        _state.update { it.copy(isLoading = true, error = null, step = WizardStep.SETTING_UP) }
        viewModelScope.launch {
            val token = _state.value.accessToken
            val dbPassword = UUID.randomUUID().toString()
            val projectName = "HealthDispatch-${UUID.randomUUID().toString().take(8)}"

            val createResult = managementApi.createProject(
                accessToken = token,
                name = projectName,
                organizationId = organizationId,
                dbPassword = dbPassword,
                region = "us-east-1"
            )

            createResult.fold(
                onSuccess = { project ->
                    // Get API keys for the new project
                    val keysResult = managementApi.getApiKeys(token, project.id)
                    keysResult.fold(
                        onSuccess = { keys ->
                            val anonKey = keys.firstOrNull { it.name == "anon" }
                            if (anonKey != null) {
                                val projectUrl = "https://${project.id}.supabase.co"

                                // Run database migrations
                                managementApi.runSql(token, project.id, DatabaseMigrations.migrationSql)

                                cloudConfigRepository.saveCloudConfig(projectUrl, anonKey.apiKey)
                                _state.update { it.copy(step = WizardStep.COMPLETE, isLoading = false) }
                            } else {
                                _state.update {
                                    it.copy(
                                        step = WizardStep.CREATE_PROJECT,
                                        isLoading = false,
                                        error = "Project created but no API key found"
                                    )
                                }
                            }
                        },
                        onFailure = { error ->
                            _state.update {
                                it.copy(
                                    step = WizardStep.CREATE_PROJECT,
                                    isLoading = false,
                                    error = error.message ?: "Failed to get API keys"
                                )
                            }
                        }
                    )
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            step = WizardStep.CREATE_PROJECT,
                            isLoading = false,
                            error = error.message ?: "Failed to create project"
                        )
                    }
                }
            )
        }
    }

    fun goToCreateProject() {
        _state.update { it.copy(step = WizardStep.CREATE_PROJECT, error = null) }
    }

    fun goBackToProjectList() {
        _state.update { it.copy(step = WizardStep.SELECT_PROJECT, error = null) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
