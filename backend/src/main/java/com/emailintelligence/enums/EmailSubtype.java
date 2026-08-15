package com.emailintelligence.enums;

/**
 * Fine-grained subtype within a category.
 * Matches the Postgres ENUM 'email_subtype'.
 */
public enum EmailSubtype {
    // Recruiter subtypes
    INTERVIEW_SCHEDULED,
    REJECTION,
    NEXT_ROUND,
    ASSESSMENT_REQUIRED,
    OFFER,
    RECRUITER_OTHER,

    // Bank subtypes
    STATEMENT_GENERATED,
    ACCOUNT_ALERT,
    BANK_OTHER,

    // Personal subtypes
    FAMILY_FRIEND,
    PERSONAL_OTHER,

    // Business subtypes
    CLIENT_VENDOR,
    WORK_OFFICIAL,
    BUSINESS_OTHER,

    // Delivery subtypes
    DELIVERY_ISSUE,
    DELIVERY_STATUS,

    // Catch-all
    OTHER_IMPORTANT_MISC
}