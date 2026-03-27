package com.healthdispatch.data.auth

import kotlinx.coroutines.flow.StateFlow

sealed class AuthState {
    data object Unknown : AuthState()
    data object Authenticated : AuthState()
    data object Unauthenticated : AuthState()
}

interface AuthRepository {
    val authState: StateFlow<AuthState>
    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun signUp(email: String, password: String): Result<Unit>
    suspend fun signInWithGoogle(idToken: String): Result<Unit>
    suspend fun signInWithFacebook(accessToken: String): Result<Unit>
    suspend fun signOut(): Result<Unit>
    suspend fun refreshAuthState()
}
