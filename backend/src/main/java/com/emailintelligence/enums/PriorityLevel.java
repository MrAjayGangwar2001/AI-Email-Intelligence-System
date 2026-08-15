package com.emailintelligence.enums;

/**
 * Matches the Postgres ENUM 'priority_level'.
 * Only used for STORED (important) emails, so LOW is intentionally absent.
 */
public enum PriorityLevel {
    CRITICAL,   // action needed within hours (interview tomorrow, deadline today)
    HIGH,       // action needed soon (assessment due in days)
    MEDIUM      // informational but important (rejection, statement)
}
