package com.healthdispatch.data.cloud

object DatabaseMigrations {

    val migrationSql: String = """
        -- Steps records table
        CREATE TABLE IF NOT EXISTS steps_records (
            id TEXT PRIMARY KEY,
            user_id UUID NOT NULL REFERENCES auth.users(id),
            type TEXT NOT NULL,
            value BIGINT,
            start_time TIMESTAMPTZ,
            end_time TIMESTAMPTZ,
            last_modified TIMESTAMPTZ NOT NULL,
            created_at TIMESTAMPTZ DEFAULT NOW()
        );

        -- Heart rate records table
        CREATE TABLE IF NOT EXISTS heart_rate_records (
            id TEXT PRIMARY KEY,
            user_id UUID NOT NULL REFERENCES auth.users(id),
            type TEXT NOT NULL,
            bpm BIGINT,
            start_time TIMESTAMPTZ,
            end_time TIMESTAMPTZ,
            last_modified TIMESTAMPTZ NOT NULL,
            created_at TIMESTAMPTZ DEFAULT NOW()
        );

        -- Enable Row Level Security
        ALTER TABLE steps_records ENABLE ROW LEVEL SECURITY;
        ALTER TABLE heart_rate_records ENABLE ROW LEVEL SECURITY;

        -- RLS policies: users can only access their own data
        DO ${'$'}${'$'}
        BEGIN
            IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'steps_records_user_policy') THEN
                CREATE POLICY steps_records_user_policy ON steps_records
                    FOR ALL USING (auth.uid() = user_id);
            END IF;

            IF NOT EXISTS (SELECT 1 FROM pg_policies WHERE policyname = 'heart_rate_records_user_policy') THEN
                CREATE POLICY heart_rate_records_user_policy ON heart_rate_records
                    FOR ALL USING (auth.uid() = user_id);
            END IF;
        END
        ${'$'}${'$'};
    """.trimIndent()
}
