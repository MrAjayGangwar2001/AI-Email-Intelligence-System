package com.emailintelligence.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Payload n8n POSTs to /api/emails/analyze after pulling a new email from Gmail.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IncomingEmailRequest {

    @NotBlank
    private String gmailMessageId;

    private String gmailThreadId;

    private String senderName;

    @NotBlank
    private String senderEmail;

    private String subject;

    @NotBlank
    private String bodyText;

    private Boolean hasAttachments;

    @NotNull
    private OffsetDateTime receivedAt;
}
