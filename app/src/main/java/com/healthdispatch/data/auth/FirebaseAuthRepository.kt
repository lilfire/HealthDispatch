package com.healthdispatch.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.FirebaseNetworkException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unknown)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        firebaseAuth.addAuthStateListener { auth ->
            _authState.value = if (auth.currentUser != null) {
                AuthState.Authenticated
            } else {
                AuthState.Unauthenticated
            }
        }
    }

    override suspend fun refreshAuthState() {
        _authState.value = if (firebaseAuth.currentUser != null) {
            AuthState.Authenticated
        } else {
            AuthState.Unauthenticated
        }
    }

    override suspend fun signIn(email: String, password: String): Result<Unit> {
        return try {
            firebaseAuth.signInWithEmailAndPassword(email, password).await()
            _authState.value = AuthState.Authenticated
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(mapFirebaseError(e)))
        }
    }

    override suspend fun signUp(email: String, password: String): Result<Unit> {
        return try {
            firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            _authState.value = AuthState.Authenticated
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(mapFirebaseError(e)))
        }
    }

    override suspend fun signInWithGoogle(idToken: String, nonce: String): Result<Unit> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            firebaseAuth.signInWithCredential(credential).await()
            _authState.value = AuthState.Authenticated
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(mapFirebaseError(e)))
        }
    }

    override suspend fun signInWithApple(idToken: String): Result<Unit> {
        return try {
            val provider = OAuthProvider.newBuilder("apple.com")
            val credential = provider.build().credential
                ?: return Result.failure(Exception("Failed to build Apple credential"))
            firebaseAuth.signInWithCredential(credential).await()
            _authState.value = AuthState.Authenticated
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(mapFirebaseError(e)))
        }
    }

    override suspend fun signInWithFacebook(accessToken: String): Result<Unit> {
        return try {
            val credential = com.google.firebase.auth.FacebookAuthProvider.getCredential(accessToken)
            firebaseAuth.signInWithCredential(credential).await()
            _authState.value = AuthState.Authenticated
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception(mapFirebaseError(e)))
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            firebaseAuth.signOut()
            _authState.value = AuthState.Unauthenticated
            Result.success(Unit)
        } catch (e: Exception) {
            _authState.value = AuthState.Unauthenticated
            Result.success(Unit)
        }
    }

    private fun mapFirebaseError(e: Exception): String {
        return when (e) {
            is FirebaseAuthInvalidCredentialsException ->
                "The email or password you entered is incorrect"
            is FirebaseAuthInvalidUserException ->
                "The email or password you entered is incorrect"
            is FirebaseAuthUserCollisionException ->
                "An account with this email already exists. Try signing in instead"
            is FirebaseAuthWeakPasswordException ->
                "Password must be at least 6 characters long"
            is FirebaseNetworkException ->
                "No internet connection. Please check your network and try again"
            else -> e.message ?: "Authentication failed"
        }
    }
}
