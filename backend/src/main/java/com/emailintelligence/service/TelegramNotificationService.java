package com.emailintelligence.service;

import com.emailintelligence.config.AppProperties;
import com.emailintelligence.entity.ImportantEmail;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramNotificationService {

    private final WebClient telegramWebClient;
    private final AppProperties appProperties;

    private static final DateTimeFormatter DEADLINE_FORMAT = DateTimeFormatter.ofPattern("dd MMM, hh:mm a");

    public String send(ImportantEmail email) {
        String text = buildMessage(email);
        try {
            return sendText(text);
        } catch (IllegalStateException e) {
            throw new IllegalStateException(e.getMessage() + " (email: " + email.getId() + ")", e);
        }
    }

    /**
     * Sends a raw pre-formatted (HTML-parse-mode) message to the configured
     * Telegram chat. Used directly by the reminder and daily-digest schedulers.
     * Returns the Telegram message_id on success, throws on failure.
     */
    public String sendText(String text) {
        String botToken = appProperties.getTelegram().getBotToken();
        String chatId = appProperties.getTelegram().getChatId();

        if (botToken == null || botToken.isBlank() || chatId == null || chatId.isBlank()) {
            throw new IllegalStateException("Telegram bot token / chat ID not configured");
        }

        Map<String, Object> body = Map.of(
                "chat_id", chatId,
                "text", text,
                "parse_mode", "HTML",
                "disable_web_page_preview", true
        );

        TelegramSendResponse response = telegramWebClient.post()
                .uri("/bot{token}/sendMessage", botToken)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(TelegramSendResponse.class)
                .block();

        if (response == null || !response.ok() || response.result() == null) {
            throw new IllegalStateException("Telegram API did not confirm delivery");
        }

        String messageId = String.valueOf(response.result().messageId());
        log.info("Telegram message sent (message_id={})", messageId);
        return messageId;
    }

    private String buildMessage(ImportantEmail email) {
        String icon = switch (email.getPriority()) {
            case CRITICAL -> "\uD83D\uDD34"; // red circle
            case HIGH -> "\uD83D\uDFE0";     // orange circle
            case MEDIUM -> "\uD83D\uDFE1";   // yellow circle
        };

        StringBuilder sb = new StringBuilder();
        sb.append(icon).append(" <b>").append(escape(email.getSubject())).append("</b>\n\n");

        if (email.getCategory() != null) {
            sb.append("Category: ").append(email.getCategory()).append(" (").append(email.getSubtype()).append(")\n");
        }
        if (email.getCompany() != null) {
            sb.append("Company: ").append(escape(email.getCompany())).append("\n");
        }
        if (email.getJobRole() != null) {
            sb.append("Role: ").append(escape(email.getJobRole())).append("\n");
        }
        if (email.getDeadline() != null) {
            sb.append("Deadline: ").append(email.getDeadline().format(DEADLINE_FORMAT)).append("\n");
        }
        sb.append("\n").append(escape(email.getSummary()));
        if (email.getNextStep() != null && !email.getNextStep().isBlank()) {
            sb.append("\n\n\u27A1\uFE0F <b>Next step:</b> ").append(escape(email.getNextStep()));
        }
        sb.append("\n\nFrom: ").append(escape(email.getSenderEmail()));

        return sb.toString();
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // ---- minimal DTOs for Telegram's response shape (snake_case from Telegram API) ----
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TelegramSendResponse(boolean ok, TelegramMessageResult result) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TelegramMessageResult(@JsonProperty("message_id") long messageId) {}

}