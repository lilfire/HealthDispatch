package com.healthdispatch.data.cloud

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CloudConfigTest {

    @Test
    fun `CloudConfig stores url and apiKey`() {
        val config = CloudConfig(url = "https://example.supabase.co", apiKey = "test-key-123")

        assertEquals("https://example.supabase.co", config.url)
        assertEquals("test-key-123", config.apiKey)
    }

    @Test
    fun `CloudConfig supports empty strings`() {
        val config = CloudConfig(url = "", apiKey = "")

        assertEquals("", config.url)
        assertEquals("", config.apiKey)
    }

    @Test
    fun `CloudConfig equality works correctly`() {
        val config1 = CloudConfig(url = "https://a.co", apiKey = "key1")
        val config2 = CloudConfig(url = "https://a.co", apiKey = "key1")
        val config3 = CloudConfig(url = "https://b.co", apiKey = "key2")

        assertEquals(config1, config2)
        assertNotEquals(config1, config3)
    }

    @Test
    fun `CloudConfig copy works correctly`() {
        val original = CloudConfig(url = "https://a.co", apiKey = "key1")
        val copied = original.copy(apiKey = "key2")

        assertEquals("https://a.co", copied.url)
        assertEquals("key2", copied.apiKey)
    }
}
