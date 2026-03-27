package com.healthdispatch.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

sealed class GoogleSignInError {
    data object UserCancelled : GoogleSignInError()
    data object NoCredentials : GoogleSignInError()
    data object NetworkError : GoogleSignInError()
    data class Unknown(val message: String) : GoogleSignInError()
}

object GoogleSignInHelper {

    fun parseIdToken(idToken: String?): Result<String> {
        return if (idToken.isNullOrBlank()) {
            Result.failure(Exception("No Google ID token received"))
        } else {
            Result.success(idToken)
        }
    }

    fun mapCredentialError(error: GoogleSignInError): String {
        return when (error) {
            is GoogleSignInError.UserCancelled -> "Sign-in was cancelled"
            is GoogleSignInError.NoCredentials ->
                "No Google accounts found. Please add a Google account to your device"
            is GoogleSignInError.NetworkError ->
                "Network error. Please check your connection and try again"
            is GoogleSignInError.Unknown ->
                "Google sign-in failed: ${error.message}"
        }
    }

    fun buildGetCredentialRequest(googleClientId: String): GetCredentialRequest {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(googleClientId)
            .build()
        return GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
    }

    fun extractIdToken(response: GetCredentialResponse): Result<String> {
        return try {
            val credential = response.credential
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            parseIdToken(googleIdTokenCredential.idToken)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to extract Google ID token: ${e.message}"))
        }
    }

    fun classifyError(e: Exception): GoogleSignInError {
        return when (e) {
            is GetCredentialCancellationException -> GoogleSignInError.UserCancelled
            is NoCredentialException -> GoogleSignInError.NoCredentials
            is java.io.IOException -> GoogleSignInError.NetworkError
            else -> GoogleSignInError.Unknown(e.message ?: "Unknown error")
        }
    }

    suspend fun signIn(
        context: Context,
        googleClientId: String
    ): Result<String> {
        if (googleClientId.isBlank()) {
            return Result.failure(Exception("Google Client ID is not configured"))
        }

        val credentialManager = CredentialManager.create(context)
        val request = buildGetCredentialRequest(googleClientId)

        return try {
            val response = credentialManager.getCredential(context, request)
            extractIdToken(response)
        } catch (e: GetCredentialCancellationException) {
            Result.failure(Exception(mapCredentialError(GoogleSignInError.UserCancelled)))
        } catch (e: NoCredentialException) {
            Result.failure(Exception(mapCredentialError(GoogleSignInError.NoCredentials)))
        } catch (e: java.io.IOException) {
            Result.failure(Exception(mapCredentialError(GoogleSignInError.NetworkError)))
        } catch (e: Exception) {
            Result.failure(Exception(mapCredentialError(GoogleSignInError.Unknown(e.message ?: "Unknown error"))))
        }
    }
}
