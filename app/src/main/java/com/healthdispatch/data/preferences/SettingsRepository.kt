package com.healthdispatch.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    val supabaseUrl: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_SUPABASE_URL] ?: ""
    }

    val supabaseApiKey: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_SUPABASE_API_KEY] ?: ""
    }

    suspend fun saveSupabaseConfig(url: String, apiKey: String) {
        dataStore.edit { prefs ->
            prefs[KEY_SUPABASE_URL] = url
            prefs[KEY_SUPABASE_API_KEY] = apiKey
        }
    }

    companion object {
        val KEY_SUPABASE_URL = stringPreferencesKey("supabase_url")
        val KEY_SUPABASE_API_KEY = stringPreferencesKey("supabase_api_key")
    }
}
