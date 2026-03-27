package com.healthdispatch.data.auth

import app.cash.turbine.test
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FirebaseAuthRepositoryTest {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var repository: FirebaseAuthRepository

    @Before
    fun setup() {
        firebaseAuth = mockk(relaxed = true)
        repository = FirebaseAuthRepository(firebaseAuth)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // --- Initial state ---

    @Test
    fun `initial auth state is Unknown`() = runTest {
        assertEquals(AuthState.Unknown, repository.authState.value)
    }

    // --- refreshAuthState ---

    @Test
    fun `refreshAuthState sets Authenticated when currentUser is not null`() = runTest {
        val user = mockk<FirebaseUser>()
        every { firebaseAuth.currentUser } returns user

        repository.refreshAuthState()

        assertEquals(AuthState.Authenticated, repository.authState.value)
    }

    @Test
    fun `refreshAuthState sets Unauthenticated when currentUser is null`() = runTest {
        every { firebaseAuth.currentUser } returns null

        repository.refreshAuthState()

        assertEquals(AuthState.Unauthenticated, repository.authState.value)
    }

    // --- signIn ---

    @Test
    fun `signIn returns success on successful authentication`() = runTest {
        val authResult = mockk<AuthResult>()
        val task = mockSuccessTask(authResult)
        every { firebaseAuth.signInWithEmailAndPassword("test@example.com", "password123") } returns task

        val result = repository.signIn("test@example.com", "password123")

        assertTrue(result.isSuccess)
        assertEquals(AuthState.Authenticated, repository.authState.value)
    }

    @Test
    fun `signIn returns failure with mapped error for invalid credentials`() = runTest {
        val task = mockFailureTask<AuthResult>(
            com.google.firebase.auth.FirebaseAuthInvalidCredentialsException("ERROR_WRONG_PASSWORD", "The password is invalid")
        )
        every { firebaseAuth.signInWithEmailAndPassword("test@example.com", "wrong") } returns task

        val result = repository.signIn("test@example.com", "wrong")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("email or password") == true)
    }

    @Test
    fun `signIn returns failure with mapped error for user not found`() = runTest {
        val task = mockFailureTask<AuthResult>(
            com.google.firebase.auth.FirebaseAuthInvalidUserException("ERROR_USER_NOT_FOUND", "User not found")
        )
        every { firebaseAuth.signInWithEmailAndPassword("nobody@example.com", "pass") } returns task

        val result = repository.signIn("nobody@example.com", "pass")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("email or password") == true)
    }

    @Test
    fun `signIn returns failure for network error`() = runTest {
        val task = mockFailureTask<AuthResult>(
            com.google.firebase.FirebaseNetworkException("No internet")
        )
        every { firebaseAuth.signInWithEmailAndPassword("test@example.com", "pass") } returns task

        val result = repository.signIn("test@example.com", "pass")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("internet connection") == true)
    }

    // --- signUp ---

    @Test
    fun `signUp returns success on successful registration`() = runTest {
        val authResult = mockk<AuthResult>()
        val task = mockSuccessTask(authResult)
        every { firebaseAuth.createUserWithEmailAndPassword("new@example.com", "password123") } returns task

        val result = repository.signUp("new@example.com", "password123")

        assertTrue(result.isSuccess)
        assertEquals(AuthState.Authenticated, repository.authState.value)
    }

    @Test
    fun `signUp returns failure for existing user`() = runTest {
        val task = mockFailureTask<AuthResult>(
            com.google.firebase.auth.FirebaseAuthUserCollisionException("ERROR_EMAIL_ALREADY_IN_USE", "Email in use")
        )
        every { firebaseAuth.createUserWithEmailAndPassword("exists@example.com", "pass123") } returns task

        val result = repository.signUp("exists@example.com", "pass123")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("already exists") == true)
    }

    @Test
    fun `signUp returns failure for weak password`() = runTest {
        val task = mockFailureTask<AuthResult>(
            com.google.firebase.auth.FirebaseAuthWeakPasswordException("ERROR_WEAK_PASSWORD", "Weak password", "Password should be at least 6 characters")
        )
        every { firebaseAuth.createUserWithEmailAndPassword("test@example.com", "12") } returns task

        val result = repository.signUp("test@example.com", "12")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("at least 6 characters") == true)
    }

    // --- signInWithGoogle ---

    @Test
    fun `signInWithGoogle returns success on valid token`() = runTest {
        val authResult = mockk<AuthResult>()
        val task = mockSuccessTask(authResult)
        mockkStatic(GoogleAuthProvider::class)
        val credential = mockk<com.google.firebase.auth.AuthCredential>()
        every { GoogleAuthProvider.getCredential("google-id-token", null) } returns credential
        every { firebaseAuth.signInWithCredential(credential) } returns task

        val result = repository.signInWithGoogle("google-id-token", "test-nonce")

        assertTrue(result.isSuccess)
        assertEquals(AuthState.Authenticated, repository.authState.value)
    }

    @Test
    fun `signInWithGoogle returns failure on invalid token`() = runTest {
        val task = mockFailureTask<AuthResult>(
            com.google.firebase.auth.FirebaseAuthInvalidCredentialsException("ERROR_INVALID_CREDENTIAL", "Invalid credential")
        )
        mockkStatic(GoogleAuthProvider::class)
        val credential = mockk<com.google.firebase.auth.AuthCredential>()
        every { GoogleAuthProvider.getCredential("bad-token", null) } returns credential
        every { firebaseAuth.signInWithCredential(credential) } returns task

        val result = repository.signInWithGoogle("bad-token", "test-nonce")

        assertTrue(result.isFailure)
    }

    // --- signOut ---

    @Test
    fun `signOut clears auth state and calls firebase signOut`() = runTest {
        // First sign in
        val authResult = mockk<AuthResult>()
        val signInTask = mockSuccessTask(authResult)
        every { firebaseAuth.signInWithEmailAndPassword("test@example.com", "pass") } returns signInTask
        repository.signIn("test@example.com", "pass")
        assertEquals(AuthState.Authenticated, repository.authState.value)

        // Then sign out
        every { firebaseAuth.signOut() } just Runs

        val result = repository.signOut()

        assertTrue(result.isSuccess)
        assertEquals(AuthState.Unauthenticated, repository.authState.value)
        verify { firebaseAuth.signOut() }
    }

    // --- Auth state listener ---

    @Test
    fun `auth state listener updates state when user changes`() = runTest {
        val listenerSlot = slot<FirebaseAuth.AuthStateListener>()
        every { firebaseAuth.addAuthStateListener(capture(listenerSlot)) } just Runs

        val repo = FirebaseAuthRepository(firebaseAuth)

        // Verify listener was registered
        assertTrue(listenerSlot.isCaptured)

        // Simulate user sign-in via listener
        val user = mockk<FirebaseUser>()
        every { firebaseAuth.currentUser } returns user
        listenerSlot.captured.onAuthStateChanged(firebaseAuth)

        assertEquals(AuthState.Authenticated, repo.authState.value)

        // Simulate user sign-out via listener
        every { firebaseAuth.currentUser } returns null
        listenerSlot.captured.onAuthStateChanged(firebaseAuth)

        assertEquals(AuthState.Unauthenticated, repo.authState.value)
    }

    // --- Helper: mock Task ---

    private fun <T> mockSuccessTask(result: T): Task<T> {
        val task = mockk<Task<T>>()
        every { task.isSuccessful } returns true
        every { task.result } returns result
        every { task.exception } returns null
        every { task.isComplete } returns true
        every { task.isCanceled } returns false
        // For kotlinx-coroutines-play-services await()
        every { task.addOnSuccessListener(any()) } answers {
            val listener = firstArg<com.google.android.gms.tasks.OnSuccessListener<T>>()
            listener.onSuccess(result)
            task
        }
        every { task.addOnFailureListener(any()) } returns task
        every { task.addOnCompleteListener(any()) } answers {
            val listener = firstArg<com.google.android.gms.tasks.OnCompleteListener<T>>()
            listener.onComplete(task)
            task
        }
        return task
    }

    private fun <T> mockFailureTask(exception: Exception): Task<T> {
        val task = mockk<Task<T>>()
        every { task.isSuccessful } returns false
        every { task.result } throws exception
        every { task.exception } returns exception
        every { task.isComplete } returns true
        every { task.isCanceled } returns false
        every { task.addOnSuccessListener(any()) } returns task
        every { task.addOnFailureListener(any()) } answers {
            val listener = firstArg<com.google.android.gms.tasks.OnFailureListener>()
            listener.onFailure(exception)
            task
        }
        every { task.addOnCompleteListener(any()) } answers {
            val listener = firstArg<com.google.android.gms.tasks.OnCompleteListener<T>>()
            listener.onComplete(task)
            task
        }
        return task
    }
}
