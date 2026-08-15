package com.emailintelligence.repository;

import com.emailintelligence.entity.EmailAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface EmailActionRepository extends JpaRepository<EmailAction, UUID> {
    List<EmailAction> findByIsCompletedFalseAndDueAtBeforeOrderByDueAtAsc(OffsetDateTime cutoff);
    List<EmailAction> findByIsCompletedFalseOrderByDueAtAsc();
}
