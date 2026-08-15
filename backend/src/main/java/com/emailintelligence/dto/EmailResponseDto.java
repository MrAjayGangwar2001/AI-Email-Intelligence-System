package com.emailintelligence.dto;

import com.emailintelligence.entity.ImportantEmail;
import com.emailintelligence.enums.EmailCategory;
import com.emailintelligence.enums.EmailSubtype;
import com.emailintelligence.enums.PriorityLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * What the dashboard/React frontend actually receives - trimmed down
 * from the entity, no internal AI debug fields like ai_raw_response.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailResponseDto {

    private UUID id;
    private String senderName;
    private String senderEmail;
    private String subject;
    private String bodySnippet;
    private String bodyText;
    private OffsetDateTime receivedAt;

    private EmailCategory category;
    private EmailSubtype subtype;
    private PriorityLevel priority;
    private Short importanceScore;

    private Boolean actionRequired;
    private OffsetDateTime deadline;

    private String company;
    private String jobRole;
    private String nextStep;
    private String summary;

    private Boolean shouldNotify;
    private Boolean isRead;
    private OffsetDateTime readAt;

    public static EmailResponseDto fromEntity(ImportantEmail e) {
        return EmailResponseDto.builder()
                .id(e.getId())
                .senderName(e.getSenderName())
                .senderEmail(e.getSenderEmail())
                .subject(e.getSubject())
                .bodySnippet(e.getBodySnippet())
                .bodyText(e.getBodyText())
                .receivedAt(e.getReceivedAt())
                .category(e.getCategory())
                .subtype(e.getSubtype())
                .priority(e.getPriority())
                .importanceScore(e.getImportanceScore())
                .actionRequired(e.getActionRequired())
                .deadline(e.getDeadline())
                .company(e.getCompany())
                .jobRole(e.getJobRole())
                .nextStep(e.getNextStep())
                .summary(e.getSummary())
                .shouldNotify(e.getShouldNotify())
                .isRead(e.getIsRead())
                .readAt(e.getReadAt())
                .build();
    }
}