package com.emailintelligence.enums;

/**
 * The outcome of AI analysis for ANY email, including ones that get
 * discarded. Used only in {@code processed_message_log} for dedup/audit,
 * never attached to stored email content.
 * Matches the Postgres ENUM 'processing_result'.
 */
public enum ProcessingResult {
    RECRUITER_RESPONSE,
    BANK_IMPORTANT,
    PERSONAL_IMPORTANT,
    COMPANY_BUSINESS,
    DELIVERY_UPDATE,
    OTHER_IMPORTANT,
    IGNORED
}