package com.emailintelligence.service;

import com.emailintelligence.config.AppProperties;
import com.emailintelligence.entity.ImportantEmail;
import com.emailintelligence.repository.ImportantEmailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Periodically checks for important emails whose deadline is coming up
 * within the configured lead time (default 30 min) and sends a one-time
 * Telegram nudge. Uses the reminder_sent flag to guarantee each email is
 * only nudged once, even if the scheduler overlaps with itself.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReminderSchedulerService {

    private final ImportantEmailRepository importantEmailRepository;
    private final TelegramNotificationService telegramNotificationService;
    private final AppProperties appProperties;

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm a");

    @Scheduled(fixedRateString = "${app.reminders.check-interval-ms:300000}")
    @Transactional
    public void checkUpcomingDeadlines() {
        if (!appProperties.getReminders().isEnabled()) {
            return;
        }

        int leadMinutes = appProperties.getReminders().getLeadMinutes();
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime windowEnd = now.plusMinutes(leadMinutes);

        List<ImportantEmail> dueForReminder =
                importantEmailRepository.findByReminderSentFalseAndDeadlineBetween(now, windowEnd);

        if (dueForReminder.isEmpty()) {
            return;
        }

        log.info("Found {} email(s) needing a deadline reminder", dueForReminder.size());

        for (ImportantEmail email : dueForReminder) {
            try {
                String text = buildReminderMessage(email, leadMinutes);
                telegramNotificationService.sendText(text);
                email.setReminderSent(true);
                importantEmailRepository.save(email);
            } catch (Exception e) {
                // Don't mark as sent if delivery failed - it'll be retried on the next tick,
                // as long as the deadline hasn't passed the window yet.
                log.error("Failed to send deadline reminder for email {}", email.getId(), e);
            }
        }
    }

    private String buildReminderMessage(ImportantEmail email, int leadMinutes) {
        StringBuilder sb = new StringBuilder();
        sb.append("\u23F0 <b>Reminder:</b> ").append(escape(email.getSubject())).append("\n\n");
        sb.append("Starts at ").append(email.getDeadline().format(TIME_FORMAT))
          .append(" (in ~").append(leadMinutes).append(" min)\n");

        if (email.getCompany() != null) {
            sb.append("Company: ").append(escape(email.getCompany())).append("\n");
        }
        if (email.getNextStep() != null && !email.getNextStep().isBlank()) {
            sb.append("\n\u27A1\uFE0F ").append(escape(email.getNextStep()));
        }
        return sb.toString();
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}