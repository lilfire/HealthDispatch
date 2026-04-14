package com.healthdispatch.data.cloud

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.WriteBatch
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FirestoreHealthRepositoryTest {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var repository: FirestoreHealthRepository

    @Before
    fun setup() {
        firestore = mockk(relaxed = true)
        firebaseAuth = mockk(relaxed = true)
        repository = FirestoreHealthRepository(firestore, firebaseAuth)
    }

    // --- pushRecords ---

    @Test
    fun `pushRecords returns failure when user is not authenticated`() = runTest {
        every { firebaseAuth.currentUser } returns null

        val result = repository.pushRecords("steps", listOf("""{"count":100}"""))

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Not authenticated") == true)
    }

    @Test
    fun `pushRecords returns success with count for empty payloads`() = runTest {
        val result = repository.pushRecords("steps", emptyList())

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull())
    }

    @Test
    fun `pushRecords writes to correct Firestore collection path`() = runTest {
        val user = mockk<FirebaseUser>()
        every { user.uid } returns "user-123"
        every { firebaseAuth.currentUser } returns user

        val batch = mockk<WriteBatch>(relaxed = true)
        every { firestore.batch() } returns batch

        val collectionRef = mockk<CollectionReference>(relaxed = true)
        val docRef = mockk<DocumentReference>(relaxed = true)
        every { firestore.collection("users/user-123/steps") } returns collectionRef
        every { collectionRef.document() } returns docRef

        val commitTask = mockSuccessTask<Void>(null)
        every { batch.commit() } returns commitTask

        val result = repository.pushRecords("steps", listOf("""{"count":100,"timestamp":1234}"""))

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull())
        verify { firestore.collection("users/user-123/steps") }
    }

    @Test
    fun `pushRecords handles multiple records in a batch`() = runTest {
        val user = mockk<FirebaseUser>()
        every { user.uid } returns "user-456"
        every { firebaseAuth.currentUser } returns user

        val batch = mockk<WriteBatch>(relaxed = true)
        every { firestore.batch() } returns batch

        val collectionRef = mockk<CollectionReference>(relaxed = true)
        val docRef = mockk<DocumentReference>(relaxed = true)
        every { firestore.collection("users/user-456/steps") } returns collectionRef
        every { collectionRef.document() } returns docRef

        val commitTask = mockSuccessTask<Void>(null)
        every { batch.commit() } returns commitTask

        val payloads = listOf(
            """{"count":100}""",
            """{"count":200}""",
            """{"count":300}"""
        )

        val result = repository.pushRecords("steps", payloads)

        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrNull())
        verify(exactly = 3) { batch.set(docRef, any<Map<String, Any>>()) }
    }

    @Test
    fun `pushRecords returns failure when batch commit fails`() = runTest {
        val user = mockk<FirebaseUser>()
        every { user.uid } returns "user-789"
        every { firebaseAuth.currentUser } returns user

        val batch = mockk<WriteBatch>(relaxed = true)
        every { firestore.batch() } returns batch

        val collectionRef = mockk<CollectionReference>(relaxed = true)
        val docRef = mockk<DocumentReference>(relaxed = true)
        every { firestore.collection("users/user-789/steps") } returns collectionRef
        every { collectionRef.document() } returns docRef

        val commitTask = mockFailureTask<Void>(RuntimeException("Firestore write failed"))
        every { batch.commit() } returns commitTask

        val result = repository.pushRecords("steps", listOf("""{"count":100}"""))

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Firestore write failed") == true)
    }

    @Test
    fun `pushRecords preserves all fields from JSON payload`() = runTest {
        val user = mockk<FirebaseUser>()
        every { user.uid } returns "user-abc"
        every { firebaseAuth.currentUser } returns user

        val batch = mockk<WriteBatch>(relaxed = true)
        every { firestore.batch() } returns batch

        val collectionRef = mockk<CollectionReference>(relaxed = true)
        val docRef = mockk<DocumentReference>(relaxed = true)
        every { firestore.collection("users/user-abc/heart_rate") } returns collectionRef
        every { collectionRef.document() } returns docRef

        val commitTask = mockSuccessTask<Void>(null)
        every { batch.commit() } returns commitTask

        val payload = """{"bpm":72,"timestamp":1711500000,"source":"health_connect"}"""

        val result = repository.pushRecords("heart_rate", listOf(payload))

        assertTrue(result.isSuccess)
        verify {
            batch.set(docRef, match<Map<String, Any>> { map ->
                map.containsKey("bpm") && map.containsKey("timestamp") && map.containsKey("source")
            })
        }
    }

    // --- Helper: mock Task ---

    private fun <T> mockSuccessTask(result: T?): Task<T> {
        val task = mockk<Task<T>>()
        every { task.isSuccessful } returns true
        every { task.result } returns result as T
        every { task.exception } returns null
        every { task.isComplete } returns true
        every { task.isCanceled } returns false
        every { task.addOnSuccessListener(any()) } answers {
            val listener = firstArg<com.google.android.gms.tasks.OnSuccessListener<T>>()
            listener.onSuccess(result as T)
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
