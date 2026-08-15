package com.emailintelligence.entity;

import com.emailintelligence.enums.EmailCategory;
import com.emailintelligence.enums.EmailSubtype;
import com.emailintelligence.enums.PriorityLevel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Full email content + AI analysis, combined into one row.
 * Only rows classified as RECRUITER_RESPONSE or BANK_IMPORTANT
 * ever land here (see ProcessedMessageLog for the IGNORED path).
 */
@Entity
@Table(name = "important_emails")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportantEmail {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "gmail_message_id", nullable = false, unique = true)
    private String gmailMessageId;

    @Column(name = "gmail_thread_id")
    private String gmailThreadId;

    // ---- Raw email fields ----
    @Column(name = "sender_name")
    private String senderName;

    @Column(name = "sender_email", nullable = false)
    private String senderEmail;

    @Column(name = "subject", columnDefinition = "TEXT")
    private String subject;

    @Column(name = "body_text", columnDefinition = "TEXT")
    private String bodyText;

    @Column(name = "body_snippet", length = 500)
    private String bodySnippet;

    @Column(name = "has_attachments")
    private Boolean hasAttachments;

    @Column(name = "received_at", nullable = false)
    private OffsetDateTime receivedAt;

    // ---- AI analysis fields ----
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "category", nullable = false)
    private EmailCategory category;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "subtype", nullable = false)
    private EmailSubtype subtype;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "priority", nullable = false)
    private PriorityLevel priority;

    @Column(name = "importance_score")
    private Short importanceScore;

    @Column(name = "action_required", nullable = false)
    private Boolean actionRequired;

    @Column(name = "deadline")
    private OffsetDateTime deadline;

    @Column(name = "company")
    private String company;

    @Column(name = "job_role")
    private String jobRole;

    @Column(name = "next_step", columnDefinition = "TEXT")
    private String nextStep;

    @Column(name = "summary", nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "should_notify", nullable = false)
    private Boolean shouldNotify;

    @Column(name = "reminder_sent", nullable = false)
    private Boolean reminderSent;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "read_at")
    private OffsetDateTime readAt;

    @Column(name = "ai_model_used")
    private String aiModelUsed;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ai_raw_response", columnDefinition = "jsonb")
    private String aiRawResponse;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "analyzed_at", nullable = false)
    private OffsetDateTime analyzedAt;
}