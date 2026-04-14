package com.healthdispatch.data.auth

import app.cash.turbine.test
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
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
    private val testScope = TestScope(testDispatcher)

    private val listenerSlot = slot<FirebaseAuth.AuthStateListener>()

    @Before
    fun setup() {
        firebaseAuth = mockk()
        mockUser = mockk()
        mockAuthResult = mockk()

        every { firebaseAuth.addAuthStateListener(capture(listenerSlot)) } answers {
            // Immediately fire with current user = null (unauthenticated)
            every { firebaseAuth.currentUser } returns null
            listenerSlot.captured.onAuthStateChanged(firebaseAuth)
        }
        every { firebaseAuth.removeAuthStateListener(any()) } just runs
    }

    private fun createRepo() = FirebaseAuthRepository(firebaseAuth, testScope)

    @Test
    fun `initial authState is Unknown before listener fires`() = testScope.runTest {
        // Without collecting the flow, initial value is Unknown
        val repo = createRepo()
        assertEquals(AuthState.Unknown, repo.authState.value)
    }

    @Test
    fun `authState emits Unauthenticated when no user`() = testScope.runTest {
        val repo = createRepo()
        repo.authState.test {
            assertEquals(AuthState.Unauthenticated, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `authState emits Authenticated when user present`() = testScope.runTest {
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
    fun `signInWithGoogle succeeds`() = testScope.runTest {
        every { firebaseAuth.signInWithCredential(any()) } returns Tasks.forResult(mockAuthResult)
        val repo = createRepo()
        val result = repo.signInWithGoogle("valid-google-id-token")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `signInWithGoogle failure returns Result failure`() = testScope.runTest {
        val exception = Exception("Google auth failed")
        every { firebaseAuth.signInWithCredential(any()) } returns Tasks.forException(exception)
        val repo = createRepo()
        val result = repo.signInWithGoogle("invalid-token")
        assertTrue(result.isFailure)
        assertEquals("Google auth failed", result.exceptionOrNull()?.message)
    }

    @Test
    fun `signInWithFacebook succeeds`() = testScope.runTest {
        every { firebaseAuth.signInWithCredential(any()) } returns Tasks.forResult(mockAuthResult)
        val repo = createRepo()
        val result = repo.signInWithFacebook("valid-facebook-access-token")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `signInWithFacebook failure returns Result failure`() = testScope.runTest {
        val exception = Exception("Facebook auth failed")
        every { firebaseAuth.signInWithCredential(any()) } returns Tasks.forException(exception)
        val repo = createRepo()
        val result = repo.signInWithFacebook("invalid-fb-token")
        assertTrue(result.isFailure)
        assertEquals("Facebook auth failed", result.exceptionOrNull()?.message)
    }

    @Test
    fun `signOut calls firebaseAuth signOut`() = testScope.runTest {
        every { firebaseAuth.signOut() } just runs
        val repo = createRepo()
        val result = repo.signOut()
        assertTrue(result.isSuccess)
        verify { firebaseAuth.signOut() }
    }

    @Test
    fun `signOut returns success even on exception`() = testScope.runTest {
        every { firebaseAuth.signOut() } throws RuntimeException("sign out error")
        val repo = createRepo()
        val result = repo.signOut()
        assertTrue(result.isFailure)
    }

    @Test
    fun `refreshAuthState does not throw`() = testScope.runTest {
        val repo = createRepo()
        repo.refreshAuthState() // Firebase listener handles state — should be a no-op
    }
}
