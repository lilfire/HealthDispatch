package com.healthdispatch.data.cloud

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CloudConfigTest {

    @Test
    fun cloudConfig_storesUrlAndApiKey() {
        val config = CloudConfig(url = "https://test.supabase.co", apiKey = "test-key-123")
        assertEquals("https://test.supabase.co", config.url)
        assertEquals("test-key-123", config.apiKey)
    }

    @Test
    fun cloudConfig_equalityForSameValues() {
        val config1 = CloudConfig(url = "https://test.supabase.co", apiKey = "key")
        val config2 = CloudConfig(url = "https://test.supabase.co", apiKey = "key")
        assertEquals(config1, config2)
    }

    @Test
    fun cloudConfig_inequalityForDifferentUrl() {
        val config1 = CloudConfig(url = "https://a.supabase.co", apiKey = "key")
        val config2 = CloudConfig(url = "https://b.supabase.co", apiKey = "key")
        assertNotEquals(config1, config2)
    }

    @Test
    fun cloudConfig_inequalityForDifferentApiKey() {
        val config1 = CloudConfig(url = "https://test.supabase.co", apiKey = "key1")
        val config2 = CloudConfig(url = "https://test.supabase.co", apiKey = "key2")
        assertNotEquals(config1, config2)
    }

    @Test
    fun cloudConfig_copyWithModifiedUrl() {
        val original = CloudConfig(url = "https://old.supabase.co", apiKey = "key")
        val modified = original.copy(url = "https://new.supabase.co")
        assertEquals("https://new.supabase.co", modified.url)
        assertEquals("key", modified.apiKey)
    }

    @Test
    fun cloudConfig_allowsEmptyValues() {
        val config = CloudConfig(url = "", apiKey = "")
        assertEquals("", config.url)
        assertEquals("", config.apiKey)
    }
}
