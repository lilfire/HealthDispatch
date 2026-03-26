package com.healthdispatch.data.cloud

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import javax.inject.Singleton

interface PostgrestClientWrapper {
    suspend fun insert(tableName: String, jsonBody: String)
}

@Singleton
class SupabasePostgrestClientWrapper @Inject constructor(
    private val supabase: SupabaseClient
) : PostgrestClientWrapper {
    override suspend fun insert(tableName: String, jsonBody: String) {
        supabase.postgrest.from(tableName).insert(jsonBody)
    }
}
