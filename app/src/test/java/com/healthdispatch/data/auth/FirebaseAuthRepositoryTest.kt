package com.healthdispatch.data.auth

import app.cash.turbine.test
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FirebaseAuthRepositoryTest {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var mockUser: FirebaseUser
    private lateinit var mockAuthResult: AuthResult
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repoScope: CoroutineScope

    private val listenerSlot = slot<FirebaseAuth.AuthStateListener>()

    @Before
    fun setup() {
        repoScope = CoroutineScope(testDispatcher)
        firebaseAuth = mockk(relaxed = true)
        mockUser = mockk()
        mockAuthResult = mockk()

        every { firebaseAuth.addAuthStateListener(capture(listenerSlot)) } answers {
            every { firebaseAuth.currentUser } returns null
            listenerSlot.captured.onAuthStateChanged(firebaseAuth)
        }
        every { firebaseAuth.removeAuthStateListener(any()) } just Runs
    }

    @After
    fun tearDown() {
        repoScope.cancel()
        unmockkAll()
    }

    private fun createRepo() = FirebaseAuthRepository(firebaseAuth, repoScope)

    @Test
    fun `authState emits Unauthenticated when no user`() = runTest {
        val repo = createRepo()
        repo.authState.test {
            assertEquals(AuthState.Unauthenticated, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `authState emits Authenticated when user present`() = runTest {
        every { firebaseAuth.addAuthStateListener(capture(listenerSlot)) } answers {
            every { firebaseAuth.currentUser } returns mockUser
            listenerSlot.captured.onAuthStateChanged(firebaseAuth)
        }

        val repo = createRepo()
        repo.authState.test {
            assertEquals(AuthState.Authenticated, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `signInWithGoogle succeeds`() = runTest {
        val task = mockSuccessTask(mockAuthResult)
        mockkStatic(GoogleAuthProvider::class)
        val credential = mockk<AuthCredential>()
        every { GoogleAuthProvider.getCredential("valid-google-id-token", null) } returns credential
        every { firebaseAuth.signInWithCredential(credential) } returns task

        val repo = createRepo()
        val result = repo.signInWithGoogle("valid-google-id-token")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `signInWithGoogle failure returns Result failure`() = runTest {
        val exception = Exception("Google auth failed")
        val task = mockFailureTask<AuthResult>(exception)
        mockkStatic(GoogleAuthProvider::class)
        val credential = mockk<AuthCredential>()
        every { GoogleAuthProvider.getCredential("invalid-token", null) } returns credential
        every { firebaseAuth.signInWithCredential(credential) } returns task

        val repo = createRepo()
        val result = repo.signInWithGoogle("invalid-token")
        assertTrue(result.isFailure)
    }

    @Test
    fun `signInWithFacebook succeeds`() = runTest {
        val task = mockSuccessTask(mockAuthResult)
        mockkStatic(FacebookAuthProvider::class)
        val credential = mockk<AuthCredential>()
        every { FacebookAuthProvider.getCredential("valid-fb-token") } returns credential
        every { firebaseAuth.signInWithCredential(credential) } returns task

        val repo = createRepo()
        val result = repo.signInWithFacebook("valid-fb-token")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `signInWithFacebook failure returns Result failure`() = runTest {
        val exception = Exception("Facebook auth failed")
        val task = mockFailureTask<AuthResult>(exception)
        mockkStatic(FacebookAuthProvider::class)
        val credential = mockk<AuthCredential>()
        every { FacebookAuthProvider.getCredential("invalid-fb-token") } returns credential
        every { firebaseAuth.signInWithCredential(credential) } returns task

        val repo = createRepo()
        val result = repo.signInWithFacebook("invalid-fb-token")
        assertTrue(result.isFailure)
    }

    @Test
    fun `signOut calls firebaseAuth signOut`() = runTest {
        every { firebaseAuth.signOut() } just Runs
        val repo = createRepo()
        val result = repo.signOut()
        assertTrue(result.isSuccess)
        verify { firebaseAuth.signOut() }
    }

    @Test
    fun `signOut returns failure on exception`() = runTest {
        every { firebaseAuth.signOut() } throws RuntimeException("sign out error")
        val repo = createRepo()
        val result = repo.signOut()
        assertTrue(result.isFailure)
    }

    @Test
    fun `refreshAuthState does not throw`() = runTest {
        val repo = createRepo()
        repo.refreshAuthState()
    }

    private fun <T> mockSuccessTask(result: T): Task<T> {
        val task = mockk<Task<T>>()
        every { task.isSuccessful } returns true
        every { task.result } returns result
        every { task.exception } returns null
        every { task.isComplete } returns true
        every { task.isCanceled } returns false
        every { task.addOnSuccessListener(any<OnSuccessListener<T>>()) } answers {
            @Suppress("UNCHECKED_CAST")
            val listener = firstArg<OnSuccessListener<T>>()
            listener.onSuccess(result)
            task
        }
        every { task.addOnFailureListener(any<OnFailureListener>()) } returns task
        every { task.addOnCompleteListener(any<OnCompleteListener<T>>()) } answers {
            @Suppress("UNCHECKED_CAST")
            val listener = firstArg<OnCompleteListener<T>>()
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
        every { task.addOnSuccessListener(any<OnSuccessListener<T>>()) } returns task
        every { task.addOnFailureListener(any<OnFailureListener>()) } answers {
            val listener = firstArg<OnFailureListener>()
            listener.onFailure(exception)
            task
        }
        every { task.addOnCompleteListener(any<OnCompleteListener<T>>()) } answers {
            @Suppress("UNCHECKED_CAST")
            val listener = firstArg<OnCompleteListener<T>>()
            listener.onComplete(task)
            task
        }
        return task
    }
}
