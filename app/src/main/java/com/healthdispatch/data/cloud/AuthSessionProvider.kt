package com.healthdispatch.data.cloud

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import javax.inject.Inject
import javax.inject.Singleton

interface AuthSessionProvider {
    fun currentUserId(): String?
}

@Singleton
class SupabaseAuthSessionProvider @Inject constructor(
    private val supabase: SupabaseClient
) : AuthSessionProvider {
    override fun currentUserId(): String? {
        return supabase.auth.currentUserOrNull()?.id
    }
}
