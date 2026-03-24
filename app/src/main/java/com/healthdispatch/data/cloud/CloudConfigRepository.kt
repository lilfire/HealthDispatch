package com.healthdispatch.data.cloud

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudConfigRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private companion object {
        val KEY_SUPABASE_URL = stringPreferencesKey("supabase_url")
        val KEY_SUPABASE_API_KEY = stringPreferencesKey("supabase_api_key")
        val KEY_ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
    }

    val cloudConfigFlow: Flow<CloudConfig> = dataStore.data.map { prefs ->
        CloudConfig(
            url = prefs[KEY_SUPABASE_URL] ?: "",
            apiKey = prefs[KEY_SUPABASE_API_KEY] ?: ""
        )
    }

    val onboardingCompleteFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_COMPLETE] ?: false
    }

    suspend fun saveCloudConfig(url: String, apiKey: String) {
        dataStore.edit { prefs ->
            prefs[KEY_SUPABASE_URL] = url
            prefs[KEY_SUPABASE_API_KEY] = apiKey
            prefs[KEY_ONBOARDING_COMPLETE] = true
        }
    }

    suspend fun resetOnboarding() {
        dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_COMPLETE] = false
        }
    }

    suspend fun currentConfig(): CloudConfig {
        val prefs = dataStore.data.first()
        return CloudConfig(
            url = prefs[KEY_SUPABASE_URL] ?: "",
            apiKey = prefs[KEY_SUPABASE_API_KEY] ?: ""
        )
    }
}
