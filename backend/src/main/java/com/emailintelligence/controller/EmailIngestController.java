package com.emailintelligence.controller;

import com.emailintelligence.dto.EmailResponseDto;
import com.emailintelligence.dto.IncomingEmailRequest;
import com.emailintelligence.service.EmailAnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Called by the n8n Gmail-trigger workflow for every new email.
 */
@RestController
@RequestMapping("/api/emails")
@RequiredArgsConstructor
@Slf4j
public class EmailIngestController {

    private final EmailAnalysisService emailAnalysisService;

    @PostMapping("/analyze")
    public ResponseEntity<?> analyze(@Valid @RequestBody IncomingEmailRequest request) {
        log.info("Received email for analysis: {} <{}>", request.getSubject(), request.getGmailMessageId());

        return emailAnalysisService.processIncomingEmail(request)
                .<ResponseEntity<?>>map(dto -> ResponseEntity.ok(Map.of(
                        "stored", true,
                        "email", dto
                )))
                .orElseGet(() -> ResponseEntity.ok(Map.of(
                        "stored", false,
                        "reason", "ignored_or_duplicate"
                )));
    }
}
