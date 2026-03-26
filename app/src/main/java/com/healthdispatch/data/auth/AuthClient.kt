package com.healthdispatch.data.auth

interface AuthClient {
    suspend fun signInWithEmail(email: String, password: String): Result<Unit>
}
