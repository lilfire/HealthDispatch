package com.healthdispatch.data.cloud

import org.junit.Assert.assertTrue
import org.junit.Test

class DatabaseMigrationsTest {

    @Test
    fun `migrationSql is not blank`() {
        assertTrue(DatabaseMigrations.migrationSql.isNotBlank())
    }

    @Test
    fun `migrationSql contains steps_records table creation`() {
        assertTrue(DatabaseMigrations.migrationSql.contains("CREATE TABLE IF NOT EXISTS steps_records"))
    }

    @Test
    fun `migrationSql contains heart_rate_records table creation`() {
        assertTrue(DatabaseMigrations.migrationSql.contains("CREATE TABLE IF NOT EXISTS heart_rate_records"))
    }

    @Test
    fun `migrationSql enables row level security for steps_records`() {
        assertTrue(DatabaseMigrations.migrationSql.contains("ALTER TABLE steps_records ENABLE ROW LEVEL SECURITY"))
    }

    @Test
    fun `migrationSql enables row level security for heart_rate_records`() {
        assertTrue(DatabaseMigrations.migrationSql.contains("ALTER TABLE heart_rate_records ENABLE ROW LEVEL SECURITY"))
    }

    @Test
    fun `migrationSql creates RLS policies`() {
        assertTrue(DatabaseMigrations.migrationSql.contains("steps_records_user_policy"))
        assertTrue(DatabaseMigrations.migrationSql.contains("heart_rate_records_user_policy"))
    }

    @Test
    fun `migrationSql references auth uid for RLS`() {
        assertTrue(DatabaseMigrations.migrationSql.contains("auth.uid()"))
    }

    @Test
    fun `migrationSql has user_id column in both tables`() {
        val sections = DatabaseMigrations.migrationSql.split("CREATE TABLE IF NOT EXISTS")
        // Should have 3 sections: before first table, steps_records, heart_rate_records
        assertTrue(sections.size >= 3)
        assertTrue(sections[1].contains("user_id"))
        assertTrue(sections[2].contains("user_id"))
    }
}
