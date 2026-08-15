package com.emailintelligence.entity;

import com.emailintelligence.enums.ProcessingResult;
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

/**
 * Minimal dedup/audit record for EVERY email the AI looks at,
 * including ignored ones. Deliberately holds no email content -
 * only the Gmail message ID and the classification result.
 */
@Entity
@Table(name = "processed_message_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedMessageLog {

    @Id
    @Column(name = "gmail_message_id", nullable = false, updatable = false)
    private String gmailMessageId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "result", nullable = false)
    private ProcessingResult result;

    @CreationTimestamp
    @Column(name = "processed_at", nullable = false, updatable = false)
    private OffsetDateTime processedAt;
}