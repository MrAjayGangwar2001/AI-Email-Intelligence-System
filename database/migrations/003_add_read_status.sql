-- Adds read/unread tracking so the dashboard can show only new (unread)
-- emails, while the Inbox view can show everything (read + unread) and
-- support keyword search over the full history.

ALTER TABLE important_emails
    ADD COLUMN IF NOT EXISTS is_read BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS read_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_important_emails_is_read ON important_emails(is_read);
