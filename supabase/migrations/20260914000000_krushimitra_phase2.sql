-- ==============================================================================
-- KRUSHI-MITRA (कृषी-मित्र) - PHASE 2 DATABASE MIGRATION
-- Supabase PostgreSQL Schema with Row Level Security (RLS) & Multi-Device Sync
-- ==============================================================================

-- 1. Helper Function: Automatic updated_at timestamp maintenance
CREATE OR REPLACE FUNCTION set_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 2. PLOTS TABLE
CREATE TABLE IF NOT EXISTS plots (
    id TEXT PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    area NUMERIC(10, 2) NOT NULL,
    area_unit TEXT NOT NULL DEFAULT 'Acre',
    soil_type TEXT NOT NULL DEFAULT '',
    irrigation_type TEXT NOT NULL DEFAULT '',
    notes TEXT NOT NULL DEFAULT '',
    archived BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_plots_user_id ON plots(user_id);
CREATE INDEX IF NOT EXISTS idx_plots_updated_at ON plots(updated_at);
CREATE TRIGGER trg_plots_updated_at BEFORE UPDATE ON plots FOR EACH ROW EXECUTE FUNCTION set_updated_at_column();

ALTER TABLE plots ENABLE ROW LEVEL SECURITY;

CREATE POLICY "plots_select_own" ON plots FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "plots_insert_own" ON plots FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "plots_update_own" ON plots FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "plots_delete_own" ON plots FOR DELETE USING (auth.uid() = user_id);

-- 3. CROP ASSIGNMENTS TABLE
CREATE TABLE IF NOT EXISTS crop_assignments (
    id TEXT PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    plot_id TEXT NOT NULL REFERENCES plots(id) ON DELETE CASCADE,
    crop_name TEXT NOT NULL,
    variety TEXT NOT NULL DEFAULT '',
    planting_date TEXT NOT NULL,
    expected_harvest_date TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'PLANNED',
    perennial BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_crops_user_id ON crop_assignments(user_id);
CREATE INDEX IF NOT EXISTS idx_crops_plot_id ON crop_assignments(plot_id);
CREATE INDEX IF NOT EXISTS idx_crops_updated_at ON crop_assignments(updated_at);
CREATE TRIGGER trg_crops_updated_at BEFORE UPDATE ON crop_assignments FOR EACH ROW EXECUTE FUNCTION set_updated_at_column();

ALTER TABLE crop_assignments ENABLE ROW LEVEL SECURITY;

CREATE POLICY "crops_select_own" ON crop_assignments FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "crops_insert_own" ON crop_assignments FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "crops_update_own" ON crop_assignments FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "crops_delete_own" ON crop_assignments FOR DELETE USING (auth.uid() = user_id);

-- 4. YIELD RECORDS TABLE
CREATE TABLE IF NOT EXISTS yield_records (
    id TEXT PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    crop_assignment_id TEXT NOT NULL REFERENCES crop_assignments(id) ON DELETE CASCADE,
    date TEXT NOT NULL,
    quantity NUMERIC(10, 2) NOT NULL,
    unit TEXT NOT NULL DEFAULT 'kg',
    rate_per_unit NUMERIC(10, 2) NOT NULL DEFAULT 0.0,
    total_revenue NUMERIC(12, 2) NOT NULL DEFAULT 0.0,
    notes TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_yield_user_id ON yield_records(user_id);
CREATE INDEX IF NOT EXISTS idx_yield_crop_id ON yield_records(crop_assignment_id);
CREATE INDEX IF NOT EXISTS idx_yield_updated_at ON yield_records(updated_at);
CREATE TRIGGER trg_yield_updated_at BEFORE UPDATE ON yield_records FOR EACH ROW EXECUTE FUNCTION set_updated_at_column();

ALTER TABLE yield_records ENABLE ROW LEVEL SECURITY;

CREATE POLICY "yield_select_own" ON yield_records FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "yield_insert_own" ON yield_records FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "yield_update_own" ON yield_records FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "yield_delete_own" ON yield_records FOR DELETE USING (auth.uid() = user_id);

-- 5. WORKERS TABLE
CREATE TABLE IF NOT EXISTS workers (
    id TEXT PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    mobile_number TEXT NOT NULL DEFAULT '',
    daily_wage_rate NUMERIC(10, 2) NOT NULL DEFAULT 0.0,
    joining_date TEXT NOT NULL DEFAULT '',
    notes TEXT NOT NULL DEFAULT '',
    archived BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_workers_user_id ON workers(user_id);
CREATE INDEX IF NOT EXISTS idx_workers_updated_at ON workers(updated_at);
CREATE TRIGGER trg_workers_updated_at BEFORE UPDATE ON workers FOR EACH ROW EXECUTE FUNCTION set_updated_at_column();

ALTER TABLE workers ENABLE ROW LEVEL SECURITY;

CREATE POLICY "workers_select_own" ON workers FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "workers_insert_own" ON workers FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "workers_update_own" ON workers FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "workers_delete_own" ON workers FOR DELETE USING (auth.uid() = user_id);

-- 6. ATTENDANCE TABLE
CREATE TABLE IF NOT EXISTS attendance (
    worker_id TEXT NOT NULL REFERENCES workers(id) ON DELETE CASCADE,
    date TEXT NOT NULL,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    status TEXT NOT NULL DEFAULT 'PRESENT',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    PRIMARY KEY (user_id, worker_id, date)
);

CREATE INDEX IF NOT EXISTS idx_attendance_user_id ON attendance(user_id);
CREATE INDEX IF NOT EXISTS idx_attendance_worker_id ON attendance(worker_id);
CREATE INDEX IF NOT EXISTS idx_attendance_date ON attendance(date);
CREATE INDEX IF NOT EXISTS idx_attendance_updated_at ON attendance(updated_at);
CREATE TRIGGER trg_attendance_updated_at BEFORE UPDATE ON attendance FOR EACH ROW EXECUTE FUNCTION set_updated_at_column();

ALTER TABLE attendance ENABLE ROW LEVEL SECURITY;

CREATE POLICY "attendance_select_own" ON attendance FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "attendance_insert_own" ON attendance FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "attendance_update_own" ON attendance FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "attendance_delete_own" ON attendance FOR DELETE USING (auth.uid() = user_id);

-- 7. WORKER TRANSACTIONS TABLE
CREATE TABLE IF NOT EXISTS worker_transactions (
    id TEXT PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    worker_id TEXT NOT NULL REFERENCES workers(id) ON DELETE CASCADE,
    type TEXT NOT NULL,
    amount NUMERIC(10, 2) NOT NULL,
    date TEXT NOT NULL,
    notes TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_trans_user_id ON worker_transactions(user_id);
CREATE INDEX IF NOT EXISTS idx_trans_worker_id ON worker_transactions(worker_id);
CREATE INDEX IF NOT EXISTS idx_trans_updated_at ON worker_transactions(updated_at);
CREATE TRIGGER trg_trans_updated_at BEFORE UPDATE ON worker_transactions FOR EACH ROW EXECUTE FUNCTION set_updated_at_column();

ALTER TABLE worker_transactions ENABLE ROW LEVEL SECURITY;

CREATE POLICY "trans_select_own" ON worker_transactions FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "trans_insert_own" ON worker_transactions FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "trans_update_own" ON worker_transactions FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "trans_delete_own" ON worker_transactions FOR DELETE USING (auth.uid() = user_id);

-- 8. DAILY TASKS TABLE
CREATE TABLE IF NOT EXISTS daily_tasks (
    id TEXT PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    date TEXT NOT NULL,
    plot_id TEXT REFERENCES plots(id) ON DELETE SET NULL,
    task_type TEXT NOT NULL DEFAULT 'General Maintenance',
    description TEXT NOT NULL,
    duration_hours NUMERIC(6, 2) NOT NULL DEFAULT 0.0,
    is_completed BOOLEAN NOT NULL DEFAULT false,
    notes TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_tasks_user_id ON daily_tasks(user_id);
CREATE INDEX IF NOT EXISTS idx_tasks_plot_id ON daily_tasks(plot_id);
CREATE INDEX IF NOT EXISTS idx_tasks_updated_at ON daily_tasks(updated_at);
CREATE TRIGGER trg_tasks_updated_at BEFORE UPDATE ON daily_tasks FOR EACH ROW EXECUTE FUNCTION set_updated_at_column();

ALTER TABLE daily_tasks ENABLE ROW LEVEL SECURITY;

CREATE POLICY "tasks_select_own" ON daily_tasks FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "tasks_insert_own" ON daily_tasks FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "tasks_update_own" ON daily_tasks FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "tasks_delete_own" ON daily_tasks FOR DELETE USING (auth.uid() = user_id);

-- 9. TASK-WORKER ASSIGNMENTS TABLE
CREATE TABLE IF NOT EXISTS task_worker_assignments (
    task_id TEXT NOT NULL REFERENCES daily_tasks(id) ON DELETE CASCADE,
    worker_id TEXT NOT NULL REFERENCES workers(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    PRIMARY KEY (user_id, task_id, worker_id)
);

CREATE INDEX IF NOT EXISTS idx_task_workers_user_id ON task_worker_assignments(user_id);
CREATE INDEX IF NOT EXISTS idx_task_workers_task_id ON task_worker_assignments(task_id);
CREATE INDEX IF NOT EXISTS idx_task_workers_worker_id ON task_worker_assignments(worker_id);
CREATE INDEX IF NOT EXISTS idx_task_workers_updated_at ON task_worker_assignments(updated_at);
CREATE TRIGGER trg_task_workers_updated_at BEFORE UPDATE ON task_worker_assignments FOR EACH ROW EXECUTE FUNCTION set_updated_at_column();

ALTER TABLE task_worker_assignments ENABLE ROW LEVEL SECURITY;

CREATE POLICY "task_workers_select_own" ON task_worker_assignments FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "task_workers_insert_own" ON task_worker_assignments FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "task_workers_update_own" ON task_worker_assignments FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "task_workers_delete_own" ON task_worker_assignments FOR DELETE USING (auth.uid() = user_id);

-- 10. EXPENSES TABLE
CREATE TABLE IF NOT EXISTS expenses (
    id TEXT PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    date TEXT NOT NULL,
    category TEXT NOT NULL,
    amount NUMERIC(10, 2) NOT NULL,
    description TEXT NOT NULL,
    plot_id TEXT REFERENCES plots(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_expenses_user_id ON expenses(user_id);
CREATE INDEX IF NOT EXISTS idx_expenses_plot_id ON expenses(plot_id);
CREATE INDEX IF NOT EXISTS idx_expenses_updated_at ON expenses(updated_at);
CREATE TRIGGER trg_expenses_updated_at BEFORE UPDATE ON expenses FOR EACH ROW EXECUTE FUNCTION set_updated_at_column();

ALTER TABLE expenses ENABLE ROW LEVEL SECURITY;

CREATE POLICY "expenses_select_own" ON expenses FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "expenses_insert_own" ON expenses FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "expenses_update_own" ON expenses FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);
CREATE POLICY "expenses_delete_own" ON expenses FOR DELETE USING (auth.uid() = user_id);
