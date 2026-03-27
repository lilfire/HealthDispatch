package com.healthdispatch.data.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleSignInHelperTest {

    @Test
    fun `parseIdToken returns token from valid GoogleIdTokenCredential`() {
        val idToken = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.test-payload"
        val result = GoogleSignInHelper.parseIdToken(idToken)
        assertTrue(result.isSuccess)
        assertEquals(idToken, result.getOrNull())
    }

    @Test
    fun `parseIdToken fails for blank token`() {
        val result = GoogleSignInHelper.parseIdToken("")
        assertTrue(result.isFailure)
        assertEquals("No Google ID token received", result.exceptionOrNull()?.message)
    }

    @Test
    fun `parseIdToken fails for null token`() {
        val result = GoogleSignInHelper.parseIdToken(null)
        assertTrue(result.isFailure)
        assertEquals("No Google ID token received", result.exceptionOrNull()?.message)
    }

    @Test
    fun `mapCredentialError returns user-friendly message for cancellation`() {
        val message = GoogleSignInHelper.mapCredentialError(
            GoogleSignInError.UserCancelled
        )
        assertEquals("Sign-in was cancelled", message)
    }

    @Test
    fun `mapCredentialError returns user-friendly message for no credentials`() {
        val message = GoogleSignInHelper.mapCredentialError(
            GoogleSignInError.NoCredentials
        )
        assertEquals("No Google accounts found. Please add a Google account to your device", message)
    }

    @Test
    fun `mapCredentialError returns user-friendly message for network error`() {
        val message = GoogleSignInHelper.mapCredentialError(
            GoogleSignInError.NetworkError
        )
        assertEquals("Network error. Please check your connection and try again", message)
    }

    @Test
    fun `mapCredentialError returns generic message for unknown errors`() {
        val message = GoogleSignInHelper.mapCredentialError(
            GoogleSignInError.Unknown("Something went wrong")
        )
        assertEquals("Google sign-in failed: Something went wrong", message)
    }
}
