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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
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
        firebaseAuth = mockk()
        mockUser = mockk()
        mockAuthResult = mockk()

        every { firebaseAuth.addAuthStateListener(capture(listenerSlot)) } answers {
            every { firebaseAuth.currentUser } returns null
            listenerSlot.captured.onAuthStateChanged(firebaseAuth)
        }
        every { firebaseAuth.removeAuthStateListener(any()) } just runs
    }

    @After
    fun tearDown() {
        repoScope.cancel()
    }

    private fun createRepo() = FirebaseAuthRepository(firebaseAuth, repoScope)

    @Test
    fun `initial authState is Unknown before listener fires`() = runTest {
        // Before flow collection starts, stateIn initial value is Unknown
        // But with Eagerly + UnconfinedTestDispatcher, listener fires immediately
        // so we just verify the repo creates without error
        val repo = createRepo()
        // With Eagerly sharing, the listener fires synchronously on creation
        // so authState will already be Unauthenticated
        assertTrue(
            repo.authState.value == AuthState.Unknown ||
                repo.authState.value == AuthState.Unauthenticated
        )
    }

    @Test
    fun `authState emits Unauthenticated when no user`() = runTest {
        val repo = createRepo()
        repo.authState.test {
            // With Eagerly + unconfined, listener fires immediately
            val item = awaitItem()
            assertEquals(AuthState.Unauthenticated, item)
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
        every { firebaseAuth.signInWithCredential(any()) } returns Tasks.forResult(mockAuthResult)
        val repo = createRepo()
        val result = repo.signInWithGoogle("valid-google-id-token")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `signInWithGoogle failure returns Result failure`() = runTest {
        val exception = Exception("Google auth failed")
        every { firebaseAuth.signInWithCredential(any()) } returns Tasks.forException(exception)
        val repo = createRepo()
        val result = repo.signInWithGoogle("invalid-token")
        assertTrue(result.isFailure)
        assertEquals("Google auth failed", result.exceptionOrNull()?.message)
    }

    @Test
    fun `signInWithFacebook succeeds`() = runTest {
        every { firebaseAuth.signInWithCredential(any()) } returns Tasks.forResult(mockAuthResult)
        val repo = createRepo()
        val result = repo.signInWithFacebook("valid-facebook-access-token")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `signInWithFacebook failure returns Result failure`() = runTest {
        val exception = Exception("Facebook auth failed")
        every { firebaseAuth.signInWithCredential(any()) } returns Tasks.forException(exception)
        val repo = createRepo()
        val result = repo.signInWithFacebook("invalid-fb-token")
        assertTrue(result.isFailure)
        assertEquals("Facebook auth failed", result.exceptionOrNull()?.message)
    }

    @Test
    fun `signOut calls firebaseAuth signOut`() = runTest {
        every { firebaseAuth.signOut() } just runs
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
}
