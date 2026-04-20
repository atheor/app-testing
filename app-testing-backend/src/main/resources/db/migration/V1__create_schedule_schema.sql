-- V1: Initial schema for the SOAP scheduling platform
-- Applied by Flyway on application startup

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =====================================================
-- schedules table
-- =====================================================
CREATE TABLE IF NOT EXISTS schedules (
    id               UUID         NOT NULL DEFAULT gen_random_uuid(),
    schedule_id      VARCHAR(255) NOT NULL,
    schedule_name    VARCHAR(500) NOT NULL,
    start_date       VARCHAR(20)  NOT NULL,
    end_date         VARCHAR(20)  NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'RECEIVED',
    correlation_id   VARCHAR(255),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_schedules            PRIMARY KEY (id),
    CONSTRAINT uq_schedules_schedule_id UNIQUE      (schedule_id)
);

CREATE INDEX idx_schedules_schedule_id    ON schedules (schedule_id);
CREATE INDEX idx_schedules_correlation_id ON schedules (correlation_id);
CREATE INDEX idx_schedules_status         ON schedules (status);

-- =====================================================
-- operations table
-- =====================================================
CREATE TABLE IF NOT EXISTS operations (
    id               UUID         NOT NULL DEFAULT gen_random_uuid(),
    schedule_id      VARCHAR(255) NOT NULL,
    operation_id     VARCHAR(255) NOT NULL,
    operation_name   VARCHAR(500) NOT NULL,
    operation_type   VARCHAR(100) NOT NULL,
    payload          TEXT,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_operations          PRIMARY KEY (id),
    CONSTRAINT fk_operations_schedule FOREIGN KEY (schedule_id)
        REFERENCES schedules (schedule_id)
        ON DELETE CASCADE
);

CREATE INDEX idx_operations_operation_id ON operations (operation_id);
CREATE INDEX idx_operations_schedule_fk  ON operations (schedule_id);

-- =====================================================
-- Trigger: auto-update updated_at on schedules
-- =====================================================
CREATE OR REPLACE FUNCTION fn_set_updated_at()
    RETURNS TRIGGER LANGUAGE plpgsql AS
$$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_schedules_updated_at
    BEFORE UPDATE ON schedules
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
