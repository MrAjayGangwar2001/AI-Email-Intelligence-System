package com.emailintelligence.service;

import com.emailintelligence.config.AppProperties;
import com.emailintelligence.dto.EmailResponseDto;
import com.emailintelligence.dto.IncomingEmailRequest;
import com.emailintelligence.dto.OllamaAnalysisResult;
import com.emailintelligence.entity.EmailNotification;
import com.emailintelligence.entity.ImportantEmail;
import com.emailintelligence.entity.ProcessedMessageLog;
import com.emailintelligence.enums.EmailCategory;
import com.emailintelligence.enums.EmailSubtype;
import com.emailintelligence.enums.NotificationChannel;
import com.emailintelligence.enums.NotificationStatus;
import com.emailintelligence.enums.PriorityLevel;
import com.emailintelligence.enums.ProcessingResult;
import com.emailintelligence.repository.EmailNotificationRepository;
import com.emailintelligence.repository.ImportantEmailRepository;
import com.emailintelligence.repository.ProcessedMessageLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailAnalysisService {

    private final GroqService groqService;
    private final TelegramNotificationService telegramNotificationService;
    private final ProcessedMessageLogRepository processedMessageLogRepository;
    private final ImportantEmailRepository importantEmailRepository;
    private final EmailNotificationRepository emailNotificationRepository;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    /**
     * Full pipeline for one incoming email from n8n:
     *   1. Dedup check
     *   2. AI classification via Ollama
     *   3. If IGNORED -> log only, discard content
     *   4. If important -> persist full record
     *   5. If shouldNotify -> send Telegram message, record delivery status
     *
     * @return the stored email DTO, or empty if the email was ignored/duplicate
     */
    public Optional<EmailResponseDto> processIncomingEmail(IncomingEmailRequest request) {

        if (processedMessageLogRepository.existsById(request.getGmailMessageId())) {
            log.info("Duplicate email {} - already processed, skipping", request.getGmailMessageId());
            return Optional.empty();
        }

        OllamaAnalysisResult aiResult = groqService.analyzeEmail(request);
        ProcessingResult result = parseProcessingResult(aiResult.getResult());

        if (result == ProcessingResult.IGNORED) {
            logProcessed(request.getGmailMessageId(), ProcessingResult.IGNORED);
            log.info("Email {} classified as IGNORED: {}", request.getGmailMessageId(), aiResult.getReason());
            return Optional.empty();
        }

        logProcessed(request.getGmailMessageId(), result);
        ImportantEmail saved = saveImportantEmail(request, aiResult, result);

        if (Boolean.TRUE.equals(saved.getShouldNotify())) {
            notifyAsync(saved);
        }

        return Optional.of(EmailResponseDto.fromEntity(saved));
    }

    @Transactional
    protected ImportantEmail saveImportantEmail(IncomingEmailRequest request, OllamaAnalysisResult aiResult, ProcessingResult result) {
        String rawJson;
        try {
            rawJson = objectMapper.writeValueAsString(aiResult);
        } catch (Exception e) {
            rawJson = null;
        }

        ImportantEmail entity = ImportantEmail.builder()
                .gmailMessageId(request.getGmailMessageId())
                .gmailThreadId(request.getGmailThreadId())
                .senderName(request.getSenderName())
                .senderEmail(request.getSenderEmail())
                .subject(request.getSubject())
                .bodyText(request.getBodyText())
                .bodySnippet(snippet(request.getBodyText()))
                .hasAttachments(Boolean.TRUE.equals(request.getHasAttachments()))
                .receivedAt(request.getReceivedAt())
                .category(EmailCategory.valueOf(result.name()))
                .subtype(parseSubtype(aiResult.getSubtype()))
                .priority(parsePriority(aiResult.getPriority()))
                .importanceScore(aiResult.getImportance() == null ? null : aiResult.getImportance().shortValue())
                .actionRequired(Boolean.TRUE.equals(aiResult.getActionRequired()))
                .deadline(parseDeadline(aiResult.getDeadline()))
                .company(aiResult.getCompany())
                .jobRole(aiResult.getJobRole())
                .nextStep(aiResult.getNextStep())
                .summary(aiResult.getSummary())
                .reason(aiResult.getReason())
                .shouldNotify(Boolean.TRUE.equals(aiResult.getShouldNotify()))
                .reminderSent(false)
                .aiModelUsed(appProperties.getGroq().getModel())
                .aiRawResponse(rawJson)
                .analyzedAt(OffsetDateTime.now())
                .build();

        return importantEmailRepository.save(entity);
    }

    @Transactional
    protected void logProcessed(String gmailMessageId, ProcessingResult result) {
        ProcessedMessageLog logEntry = ProcessedMessageLog.builder()
                .gmailMessageId(gmailMessageId)
                .result(result)
                .build();
        processedMessageLogRepository.save(logEntry);
    }

    /**
     * Sends the Telegram notification and records delivery status.
     * Kept synchronous-but-isolated so a Telegram failure never blocks
     * or rolls back the already-saved email record.
     */
    protected void notifyAsync(ImportantEmail email) {
        EmailNotification notification = EmailNotification.builder()
                .email(email)
                .channel(NotificationChannel.TELEGRAM)
                .status(NotificationStatus.PENDING)
                .retryCount((short) 0)
                .build();

        try {
            String messageId = telegramNotificationService.send(email);
            notification.setStatus(NotificationStatus.SENT);
            notification.setTelegramMessageId(messageId);
            notification.setSentAt(OffsetDateTime.now());
        } catch (Exception e) {
            log.error("Failed to send Telegram notification for email {}", email.getId(), e);
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(e.getMessage());
        }

        emailNotificationRepository.save(notification);
    }

    private ProcessingResult parseProcessingResult(String value) {
        try {
            return ProcessingResult.valueOf(value.trim().toUpperCase());
        } catch (Exception e) {
            log.warn("AI returned unrecognized result '{}', defaulting to IGNORED", value);
            return ProcessingResult.IGNORED;
        }
    }

    private EmailSubtype parseSubtype(String value) {
        if (value == null) return EmailSubtype.OTHER_IMPORTANT_MISC;
        try {
            return EmailSubtype.valueOf(value.trim().toUpperCase());
        } catch (Exception e) {
            log.warn("AI returned unrecognized subtype '{}', defaulting to OTHER_IMPORTANT_MISC", value);
            return EmailSubtype.OTHER_IMPORTANT_MISC;
        }
    }

    private PriorityLevel parsePriority(String value) {
        if (value == null) return PriorityLevel.MEDIUM;
        try {
            return PriorityLevel.valueOf(value.trim().toUpperCase());
        } catch (Exception e) {
            log.warn("AI returned unrecognized priority '{}', defaulting to MEDIUM", value);
            return PriorityLevel.MEDIUM;
        }
    }

    private OffsetDateTime parseDeadline(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return OffsetDateTime.parse(value);
        } catch (Exception e) {
            log.warn("Could not parse deadline '{}', leaving null", value);
            return null;
        }
    }

    private String snippet(String bodyText) {
        if (bodyText == null) return null;
        return bodyText.length() <= 500 ? bodyText : bodyText.substring(0, 500);
    }
}