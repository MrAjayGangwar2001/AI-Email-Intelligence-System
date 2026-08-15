package com.emailintelligence.controller;

import com.emailintelligence.dto.EmailResponseDto;
import com.emailintelligence.entity.EmailAction;
import com.emailintelligence.entity.ImportantEmail;
import com.emailintelligence.enums.EmailCategory;
import com.emailintelligence.enums.PriorityLevel;
import com.emailintelligence.repository.EmailActionRepository;
import com.emailintelligence.repository.ImportantEmailRepository;
import com.emailintelligence.repository.spec.ImportantEmailSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * APIs consumed by the React dashboard.
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final ImportantEmailRepository importantEmailRepository;
    private final EmailActionRepository emailActionRepository;

    @GetMapping("/emails")
    public Page<EmailResponseDto> searchEmails(
            @RequestParam(required = false) EmailCategory category,
            @RequestParam(required = false) PriorityLevel priority,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "receivedAt"));
        var spec = ImportantEmailSpecifications.withFilters(category, priority, company, search, isRead);
        Page<ImportantEmail> results = importantEmailRepository.findAll(spec, pageable);
        return results.map(EmailResponseDto::fromEntity);
    }

    @GetMapping("/emails/{id}")
    public ResponseEntity<EmailResponseDto> getEmail(@PathVariable UUID id) {
        return importantEmailRepository.findById(id)
                .map(EmailResponseDto::fromEntity)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/emails/{id}/read")
    public ResponseEntity<EmailResponseDto> markAsRead(@PathVariable UUID id) {
        return importantEmailRepository.findById(id)
                .map(email -> {
                    if (!Boolean.TRUE.equals(email.getIsRead())) {
                        email.setIsRead(true);
                        email.setReadAt(OffsetDateTime.now());
                        email = importantEmailRepository.save(email);
                    }
                    return ResponseEntity.ok(EmailResponseDto.fromEntity(email));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/emails/{id}/unread")
    public ResponseEntity<EmailResponseDto> markAsUnread(@PathVariable UUID id) {
        return importantEmailRepository.findById(id)
                .map(email -> {
                    email.setIsRead(false);
                    email.setReadAt(null);
                    email = importantEmailRepository.save(email);
                    return ResponseEntity.ok(EmailResponseDto.fromEntity(email));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/emails/{id}")
    public ResponseEntity<Void> deleteEmail(@PathVariable UUID id) {
        if (!importantEmailRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        importantEmailRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/deadlines/upcoming")
    public List<EmailResponseDto> upcomingDeadlines(
            @RequestParam(defaultValue = "7") int days
    ) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime until = now.plusDays(days);
        return importantEmailRepository.findByDeadlineBetweenOrderByDeadlineAsc(now, until)
                .stream()
                .map(EmailResponseDto::fromEntity)
                .toList();
    }

    @GetMapping("/actions/pending")
    public List<EmailAction> pendingActions() {
        return emailActionRepository.findByIsCompletedFalseOrderByDueAtAsc();
    }

    @PatchMapping("/actions/{id}/complete")
    public ResponseEntity<EmailAction> completeAction(@PathVariable UUID id) {
        return emailActionRepository.findById(id)
                .map(action -> {
                    action.setIsCompleted(true);
                    action.setCompletedAt(OffsetDateTime.now());
                    return ResponseEntity.ok(emailActionRepository.save(action));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}