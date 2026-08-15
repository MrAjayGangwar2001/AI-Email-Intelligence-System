package com.emailintelligence.repository;

import com.emailintelligence.entity.ImportantEmail;
import com.emailintelligence.enums.EmailCategory;
import com.emailintelligence.enums.PriorityLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ImportantEmailRepository extends JpaRepository<ImportantEmail, UUID>, JpaSpecificationExecutor<ImportantEmail> {

    Optional<ImportantEmail> findByGmailMessageId(String gmailMessageId);

    Page<ImportantEmail> findByCategory(EmailCategory category, Pageable pageable);

    Page<ImportantEmail> findByPriority(PriorityLevel priority, Pageable pageable);

    List<ImportantEmail> findByDeadlineBetweenOrderByDeadlineAsc(OffsetDateTime from, OffsetDateTime to);

    List<ImportantEmail> findByReminderSentFalseAndDeadlineBetween(OffsetDateTime from, OffsetDateTime to);

    List<ImportantEmail> findByActionRequiredTrueOrderByDeadlineAsc();

    List<ImportantEmail> findByReceivedAtBetween(OffsetDateTime from, OffsetDateTime to);
}   