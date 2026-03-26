package com.healthdispatch.data.cloud

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HealthDispatchSupabaseClientTest {

    private lateinit var mockPostgrestClient: PostgrestClientWrapper
    private lateinit var mockAuthProvider: AuthSessionProvider
    private lateinit var client: HealthDispatchSupabaseClient

    @Before
    fun setUp() {
        mockPostgrestClient = mockk()
        mockAuthProvider = mockk()
        client = HealthDispatchSupabaseClient(mockPostgrestClient, mockAuthProvider)
    }

    @Test
    fun `pushRecords returns success with record count on successful insert`() = runTest {
        val userId = "user-123-uuid"
        every { mockAuthProvider.currentUserId() } returns userId
        coEvery { mockPostgrestClient.insert(any(), any()) } returns Unit

        val payloads = listOf(
            """{"id":"rec1","type":"Steps","lastModified":"2026-03-26T00:00:00Z"}""",
            """{"id":"rec2","type":"Steps","lastModified":"2026-03-26T01:00:00Z"}"""
        )

        val result = client.pushRecords("steps_records", payloads)

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull())
    }

    @Test
    fun `pushRecords injects user_id into each payload`() = runTest {
        val userId = "user-456-uuid"
        every { mockAuthProvider.currentUserId() } returns userId

        val capturedTable = slot<String>()
        val capturedPayloads = slot<String>()
        coEvery { mockPostgrestClient.insert(capture(capturedTable), capture(capturedPayloads)) } returns Unit

        val payloads = listOf(
            """{"id":"rec1","type":"Steps"}"""
        )

        client.pushRecords("steps_records", payloads)

        assertEquals("steps_records", capturedTable.captured)
        val parsed = Json.parseToJsonElement(capturedPayloads.captured).jsonArray
        assertEquals(1, parsed.size)
        assertEquals(userId, parsed[0].jsonObject["user_id"]?.jsonPrimitive?.content)
    }

    @Test
    fun `pushRecords returns failure when not authenticated`() = runTest {
        every { mockAuthProvider.currentUserId() } returns null

        val result = client.pushRecords("steps_records", listOf("""{"id":"rec1"}"""))

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Not authenticated") == true)
    }

    @Test
    fun `pushRecords returns failure when insert throws`() = runTest {
        every { mockAuthProvider.currentUserId() } returns "user-789"
        coEvery { mockPostgrestClient.insert(any(), any()) } throws RuntimeException("Network error")

        val result = client.pushRecords("steps_records", listOf("""{"id":"rec1"}"""))

        assertTrue(result.isFailure)
        assertEquals("Network error", result.exceptionOrNull()?.message)
    }

    @Test
    fun `pushRecords returns success with zero for empty payloads`() = runTest {
        every { mockAuthProvider.currentUserId() } returns "user-123"

        val result = client.pushRecords("steps_records", emptyList())

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull())
    }

    @Test
    fun `pushRecords preserves existing fields when injecting user_id`() = runTest {
        val userId = "user-999"
        every { mockAuthProvider.currentUserId() } returns userId

        val capturedPayloads = slot<String>()
        coEvery { mockPostgrestClient.insert(any(), capture(capturedPayloads)) } returns Unit

        val payloads = listOf(
            """{"id":"rec1","type":"Steps","value":42,"lastModified":"2026-03-26T00:00:00Z"}"""
        )

        client.pushRecords("steps_records", payloads)

        val parsed = Json.parseToJsonElement(capturedPayloads.captured).jsonArray[0].jsonObject
        assertEquals("rec1", parsed["id"]?.jsonPrimitive?.content)
        assertEquals("Steps", parsed["type"]?.jsonPrimitive?.content)
        assertEquals(userId, parsed["user_id"]?.jsonPrimitive?.content)
    }

    @Test
    fun `pushRecords calls insert with correct table name`() = runTest {
        every { mockAuthProvider.currentUserId() } returns "user-123"
        coEvery { mockPostgrestClient.insert(any(), any()) } returns Unit

        client.pushRecords("heart_rate_records", listOf("""{"id":"rec1"}"""))

        coVerify { mockPostgrestClient.insert("heart_rate_records", any()) }
    }
}
