package com.emailintelligence.repository;

import com.emailintelligence.entity.ProcessedMessageLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedMessageLogRepository extends JpaRepository<ProcessedMessageLog, String> {
    // gmailMessageId is the primary key, so existsById() is our dedup check.
}
