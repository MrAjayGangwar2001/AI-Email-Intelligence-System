-- ============================================================
-- AI Email Intelligence System - Database Schema
-- Target: Supabase (PostgreSQL)
--
-- DESIGN DECISION: Only IMPORTANT emails (RECRUITER_RESPONSE /
-- BANK_IMPORTANT) are stored with full content. IGNORED emails
-- (spam, promos, application confirmations, loan/credit-card
-- offers) are NEVER persisted with content — only their Gmail
-- message ID is logged, purely to prevent re-processing the
-- same email twice if n8n ever retries/re-fires.
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================
-- ENUM TYPES
-- ============================================================

-- Category for STORED (important) emails only.
CREATE TYPE email_category AS ENUM (
    'RECRUITER_RESPONSE',   -- rejection / interview / next-round / offer
    'BANK_IMPORTANT',       -- statements, important account alerts
    'PERSONAL_IMPORTANT',   -- important messages from family/known contacts
    'COMPANY_BUSINESS',     -- client/vendor/work-official business communication
    'DELIVERY_UPDATE',      -- important package/order delivery status
    'OTHER_IMPORTANT'       -- catch-all: seems important but doesn't fit above
);

CREATE TYPE email_subtype AS ENUM (
    -- Recruiter subtypes
    'INTERVIEW_SCHEDULED',
    'REJECTION',
    'NEXT_ROUND',
    'ASSESSMENT_REQUIRED',
    'OFFER',
    'RECRUITER_OTHER',
    -- Bank subtypes
    'STATEMENT_GENERATED',
    'ACCOUNT_ALERT',
    'BANK_OTHER',
    -- Personal subtypes
    'FAMILY_FRIEND',
    'PERSONAL_OTHER',
    -- Business subtypes
    'CLIENT_VENDOR',
    'WORK_OFFICIAL',
    'BUSINESS_OTHER',
    -- Delivery subtypes
    'DELIVERY_ISSUE',
    'DELIVERY_STATUS',
    -- Catch-all
    'OTHER_IMPORTANT_MISC'
);

CREATE TYPE priority_level AS ENUM (
    'CRITICAL',   -- action needed within hours (interview tomorrow, deadline today)
    'HIGH',       -- action needed soon (assessment due in days)
    'MEDIUM'      -- informational but important (rejection, statement)
);

-- Broader result enum used ONLY for the lightweight dedup log
-- (includes IGNORED, since we log the outcome but not content).
CREATE TYPE processing_result AS ENUM (
    'RECRUITER_RESPONSE',
    'BANK_IMPORTANT',
    'PERSONAL_IMPORTANT',
    'COMPANY_BUSINESS',
    'DELIVERY_UPDATE',
    'OTHER_IMPORTANT',
    'IGNORED'
);

CREATE TYPE notification_channel AS ENUM ('TELEGRAM');
CREATE TYPE notification_status AS ENUM ('PENDING', 'SENT', 'FAILED');

-- ============================================================
-- TABLE: processed_message_log
-- Minimal dedup/audit trail for EVERY email the AI looks at,
-- including ignored ones. No email content stored here.
-- ============================================================
CREATE TABLE processed_message_log (
    gmail_message_id    VARCHAR(255) PRIMARY KEY,
    result               processing_result NOT NULL,
    processed_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_processed_log_processed_at ON processed_message_log(processed_at DESC);

-- ============================================================
-- TABLE: important_emails
-- Full email content + AI analysis, combined. Only rows for
-- emails that were classified as RECRUITER_RESPONSE or
-- BANK_IMPORTANT ever land here.
-- ============================================================
CREATE TABLE important_emails (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    gmail_message_id    VARCHAR(255) NOT NULL UNIQUE REFERENCES processed_message_log(gmail_message_id),
    gmail_thread_id     VARCHAR(255),

    -- Raw email fields
    sender_name          VARCHAR(255),
    sender_email          VARCHAR(255) NOT NULL,
    subject                TEXT,
    body_text               TEXT,                          -- cleaned plain-text body (n8n strips HTML/signatures)
    body_snippet             VARCHAR(500),                    -- short preview for dashboard list view
    has_attachments           BOOLEAN DEFAULT FALSE,
    received_at                TIMESTAMPTZ NOT NULL,

    -- AI analysis fields
    category                    email_category NOT NULL,
    subtype                       email_subtype NOT NULL,
    priority                       priority_level NOT NULL,
    importance_score                 SMALLINT CHECK (importance_score BETWEEN 0 AND 100),

    action_required                    BOOLEAN NOT NULL DEFAULT FALSE,
    deadline                             TIMESTAMPTZ,          -- interview time / assessment deadline / statement due date

    company                                VARCHAR(255),         -- extracted company name (recruiter emails)
    job_role                                 VARCHAR(255),         -- extracted job title
    next_step                                  TEXT,                 -- e.g. "Join Google Meet at 10 AM"
    summary                                      TEXT NOT NULL,        -- 1-2 line AI summary
    reason                                         TEXT,                 -- why AI classified it this way (debugging/trust)

    should_notify                                    BOOLEAN NOT NULL DEFAULT FALSE,
    reminder_sent                                      BOOLEAN NOT NULL DEFAULT FALSE,   -- 30-min-before-deadline nudge already sent?

    is_read                                             BOOLEAN NOT NULL DEFAULT FALSE,   -- dashboard shows unread only; Inbox shows all
    read_at                                             TIMESTAMPTZ,

    ai_model_used                                      VARCHAR(100),         -- e.g. "mistral:7b"
    ai_raw_response                                       JSONB,                -- store raw AI JSON for debugging/re-processing

    created_at                                              TIMESTAMPTZ NOT NULL DEFAULT now(),
    analyzed_at                                               TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_important_emails_category ON important_emails(category);
CREATE INDEX idx_important_emails_priority ON important_emails(priority);
CREATE INDEX idx_important_emails_received_at ON important_emails(received_at DESC);
CREATE INDEX idx_important_emails_deadline ON important_emails(deadline) WHERE deadline IS NOT NULL;
CREATE INDEX idx_important_emails_sender_email ON important_emails(sender_email);

-- ============================================================
-- TABLE: email_notifications
-- Tracks Telegram notification delivery per stored email
-- ============================================================
CREATE TABLE email_notifications (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email_id            UUID NOT NULL REFERENCES important_emails(id) ON DELETE CASCADE,

    channel              notification_channel NOT NULL DEFAULT 'TELEGRAM',
    status                 notification_status NOT NULL DEFAULT 'PENDING',
    message_text            TEXT,
    telegram_message_id       VARCHAR(100),                    -- returned by Telegram API on success
    error_message               TEXT,
    retry_count                    SMALLINT DEFAULT 0,

    sent_at                        TIMESTAMPTZ,
    created_at                       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_notifications_email_id ON email_notifications(email_id);
CREATE INDEX idx_notifications_status ON email_notifications(status);

-- ============================================================
-- TABLE: email_actions
-- User-tracked action items / deadline tracker
-- ============================================================
CREATE TABLE email_actions (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email_id            UUID NOT NULL REFERENCES important_emails(id) ON DELETE CASCADE,

    action_description   TEXT NOT NULL,
    due_at                 TIMESTAMPTZ,
    is_completed              BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at                TIMESTAMPTZ,

    created_at                    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_actions_due_at ON email_actions(due_at) WHERE is_completed = FALSE;

-- ============================================================
-- TABLE: system_logs
-- Basic operational log for troubleshooting
-- ============================================================
CREATE TABLE system_logs (
    id                  BIGSERIAL PRIMARY KEY,
    level                VARCHAR(20) NOT NULL,             -- INFO / WARN / ERROR
    source                 VARCHAR(100),                     -- e.g. "OllamaService", "TelegramService"
    message                  TEXT NOT NULL,
    metadata                   JSONB,
    created_at                    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_logs_created_at ON system_logs(created_at DESC);
CREATE INDEX idx_logs_level ON system_logs(level);

-- ============================================================
-- VIEW: dashboard_email_view
-- Convenience view for the dashboard/API
-- ============================================================
CREATE VIEW dashboard_email_view AS
SELECT
    e.id                AS email_id,
    e.sender_name,
    e.sender_email,
    e.subject,
    e.body_snippet,
    e.received_at,
    e.category,
    e.subtype,
    e.priority,
    e.importance_score,
    e.action_required,
    e.deadline,
    e.company,
    e.job_role,
    e.next_step,
    e.summary,
    e.should_notify,
    n.status             AS notification_status
FROM important_emails e
LEFT JOIN email_notifications n ON n.email_id = e.id
ORDER BY e.received_at DESC;