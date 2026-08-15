package com.emailintelligence.service;

import com.emailintelligence.config.AppProperties;
import com.emailintelligence.entity.EmailAction;
import com.emailintelligence.entity.ImportantEmail;
import com.emailintelligence.enums.EmailCategory;
import com.emailintelligence.repository.EmailActionRepository;
import com.emailintelligence.repository.ImportantEmailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Sends one Telegram summary message per day covering:
 *  - new important emails received in the last 24 hours (by category)
 *  - deadlines coming up in the next 24 hours
 *  - open action items
 *
 * Runs on app.digest.cron (default 8:00 AM daily, configurable timezone).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DailyDigestService {

    private final ImportantEmailRepository importantEmailRepository;
    private final EmailActionRepository emailActionRepository;
    private final TelegramNotificationService telegramNotificationService;
    private final AppProperties appProperties;

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM, hh:mm a");

    @Scheduled(cron = "${app.digest.cron:0 0 8 * * *}", zone = "${app.digest.zone:Asia/Kolkata}")
    public void sendDailyDigest() {
        if (!appProperties.getDigest().isEnabled()) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime yesterday = now.minusHours(24);
        OffsetDateTime tomorrow = now.plusHours(24);

        List<ImportantEmail> last24h = importantEmailRepository.findByReceivedAtBetween(yesterday, now);
        List<ImportantEmail> upcomingDeadlines = importantEmailRepository.findByDeadlineBetweenOrderByDeadlineAsc(now, tomorrow);
        List<EmailAction> pendingActions = emailActionRepository.findByIsCompletedFalseOrderByDueAtAsc();

        // Skip sending an empty "nothing happened" digest.
        if (last24h.isEmpty() && upcomingDeadlines.isEmpty() && pendingActions.isEmpty()) {
            log.info("Skipping daily digest - nothing to report");
            return;
        }

        String message = buildDigestMessage(last24h, upcomingDeadlines, pendingActions);

        try {
            telegramNotificationService.sendText(message);
            log.info("Daily digest sent");
        } catch (Exception e) {
            log.error("Failed to send daily digest", e);
        }
    }

    private String buildDigestMessage(List<ImportantEmail> last24h, List<ImportantEmail> upcomingDeadlines, List<EmailAction> pendingActions) {
        long recruiterCount = last24h.stream().filter(e -> e.getCategory() == EmailCategory.RECRUITER_RESPONSE).count();
        long bankCount = last24h.stream().filter(e -> e.getCategory() == EmailCategory.BANK_IMPORTANT).count();
        long personalCount = last24h.stream().filter(e -> e.getCategory() == EmailCategory.PERSONAL_IMPORTANT).count();
        long businessCount = last24h.stream().filter(e -> e.getCategory() == EmailCategory.COMPANY_BUSINESS).count();
        long deliveryCount = last24h.stream().filter(e -> e.getCategory() == EmailCategory.DELIVERY_UPDATE).count();
        long otherCount = last24h.stream().filter(e -> e.getCategory() == EmailCategory.OTHER_IMPORTANT).count();

        StringBuilder sb = new StringBuilder();
        sb.append("\uD83D\uDCCB <b>Daily Digest</b>\n\n");

        sb.append("<b>Last 24 hours:</b> ").append(last24h.size()).append(" important email(s)\n");
        if (!last24h.isEmpty()) {
            if (recruiterCount > 0) sb.append("  \u2022 ").append(recruiterCount).append(" recruiter response(s)\n");
            if (bankCount > 0) sb.append("  \u2022 ").append(bankCount).append(" bank alert(s)\n");
            if (personalCount > 0) sb.append("  \u2022 ").append(personalCount).append(" personal message(s)\n");
            if (businessCount > 0) sb.append("  \u2022 ").append(businessCount).append(" business message(s)\n");
            if (deliveryCount > 0) sb.append("  \u2022 ").append(deliveryCount).append(" delivery update(s)\n");
            if (otherCount > 0) sb.append("  \u2022 ").append(otherCount).append(" other important email(s)\n");
        }

        sb.append("\n<b>Upcoming deadlines (next 24h):</b> ");
        if (upcomingDeadlines.isEmpty()) {
            sb.append("none\n");
        } else {
            sb.append("\n");
            for (ImportantEmail e : upcomingDeadlines) {
                sb.append("  \u2022 ").append(escape(e.getCompany() != null ? e.getCompany() : e.getSubject()))
                  .append(" - ").append(e.getDeadline().format(TIME_FORMAT)).append("\n");
            }
        }

        sb.append("\n<b>Pending action items:</b> ").append(pendingActions.size());

        return sb.toString();
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}