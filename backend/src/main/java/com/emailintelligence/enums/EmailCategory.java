package com.emailintelligence.enums;

/**
 * Category for STORED (important) emails only.
 * Matches the Postgres ENUM 'email_category'.
 */
public enum EmailCategory {
    RECRUITER_RESPONSE,
    BANK_IMPORTANT,
    PERSONAL_IMPORTANT,
    COMPANY_BUSINESS,
    DELIVERY_UPDATE,
    OTHER_IMPORTANT
}