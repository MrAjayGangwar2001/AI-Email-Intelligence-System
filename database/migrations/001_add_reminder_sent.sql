-- Migration: add reminder_sent column for deadline-reminder feature.
-- Run this ONLY if your database was already initialized before this
-- feature was added (i.e. docker-compose already created the postgres
-- volume once). Fresh setups get this automatically via schema.sql.

ALTER TABLE important_emails
    ADD COLUMN IF NOT EXISTS reminder_sent BOOLEAN NOT NULL DEFAULT FALSE;