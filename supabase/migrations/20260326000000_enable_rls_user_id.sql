-- Add user_id column and enable RLS on all health data tables.
-- Each table gets a policy that restricts access to the owning user.

-- steps_records
ALTER TABLE steps_records ADD COLUMN IF NOT EXISTS user_id UUID REFERENCES auth.users(id);
ALTER TABLE steps_records ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users see own data" ON steps_records FOR ALL USING (auth.uid() = user_id);

-- heart_rate_records
ALTER TABLE heart_rate_records ADD COLUMN IF NOT EXISTS user_id UUID REFERENCES auth.users(id);
ALTER TABLE heart_rate_records ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users see own data" ON heart_rate_records FOR ALL USING (auth.uid() = user_id);

-- sleep_records
ALTER TABLE sleep_records ADD COLUMN IF NOT EXISTS user_id UUID REFERENCES auth.users(id);
ALTER TABLE sleep_records ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users see own data" ON sleep_records FOR ALL USING (auth.uid() = user_id);

-- exercise_records
ALTER TABLE exercise_records ADD COLUMN IF NOT EXISTS user_id UUID REFERENCES auth.users(id);
ALTER TABLE exercise_records ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users see own data" ON exercise_records FOR ALL USING (auth.uid() = user_id);

-- body_records
ALTER TABLE body_records ADD COLUMN IF NOT EXISTS user_id UUID REFERENCES auth.users(id);
ALTER TABLE body_records ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users see own data" ON body_records FOR ALL USING (auth.uid() = user_id);

-- health_records
ALTER TABLE health_records ADD COLUMN IF NOT EXISTS user_id UUID REFERENCES auth.users(id);
ALTER TABLE health_records ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Users see own data" ON health_records FOR ALL USING (auth.uid() = user_id);
