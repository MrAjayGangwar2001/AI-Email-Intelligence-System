-- Adds new categories for emails beyond recruiter/bank:
--   PERSONAL_IMPORTANT  - important messages from family/known contacts
--   COMPANY_BUSINESS    - client/vendor/work-official business communication
--   DELIVERY_UPDATE     - important package/order delivery status
--   OTHER_IMPORTANT     - catch-all safety net for anything else important
--
-- Postgres requires each new enum value to be added in its own statement
-- (cannot be combined with usage in the same transaction), so run this
-- file as-is via psql -f (each ALTER TYPE auto-commits independently).

ALTER TYPE email_category ADD VALUE IF NOT EXISTS 'PERSONAL_IMPORTANT';
ALTER TYPE email_category ADD VALUE IF NOT EXISTS 'COMPANY_BUSINESS';
ALTER TYPE email_category ADD VALUE IF NOT EXISTS 'DELIVERY_UPDATE';
ALTER TYPE email_category ADD VALUE IF NOT EXISTS 'OTHER_IMPORTANT';

ALTER TYPE processing_result ADD VALUE IF NOT EXISTS 'PERSONAL_IMPORTANT';
ALTER TYPE processing_result ADD VALUE IF NOT EXISTS 'COMPANY_BUSINESS';
ALTER TYPE processing_result ADD VALUE IF NOT EXISTS 'DELIVERY_UPDATE';
ALTER TYPE processing_result ADD VALUE IF NOT EXISTS 'OTHER_IMPORTANT';

ALTER TYPE email_subtype ADD VALUE IF NOT EXISTS 'FAMILY_FRIEND';
ALTER TYPE email_subtype ADD VALUE IF NOT EXISTS 'PERSONAL_OTHER';
ALTER TYPE email_subtype ADD VALUE IF NOT EXISTS 'CLIENT_VENDOR';
ALTER TYPE email_subtype ADD VALUE IF NOT EXISTS 'WORK_OFFICIAL';
ALTER TYPE email_subtype ADD VALUE IF NOT EXISTS 'BUSINESS_OTHER';
ALTER TYPE email_subtype ADD VALUE IF NOT EXISTS 'DELIVERY_ISSUE';
ALTER TYPE email_subtype ADD VALUE IF NOT EXISTS 'DELIVERY_STATUS';
ALTER TYPE email_subtype ADD VALUE IF NOT EXISTS 'OTHER_IMPORTANT_MISC';
