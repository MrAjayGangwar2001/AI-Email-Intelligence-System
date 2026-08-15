package com.emailintelligence.service;

import com.emailintelligence.config.AppProperties;
import com.emailintelligence.dto.IncomingEmailRequest;
import com.emailintelligence.dto.OllamaAnalysisResult;
import com.emailintelligence.dto.OllamaGenerateRequest;
import com.emailintelligence.dto.OllamaGenerateResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.Semaphore;

@Service
@RequiredArgsConstructor
@Slf4j
public class OllamaService {

    private final WebClient ollamaWebClient;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    // This machine has very limited RAM and can only run ONE Ollama inference
    // at a time. If two emails arrive together (e.g. n8n finds 2 unread mails
    // in one poll), running both concurrently starves each other and both
    // time out. This semaphore forces requests to queue and run one-by-one.
    private final Semaphore ollamaLock = new Semaphore(1);

    private static final String SYSTEM_PROMPT = """
            You are an email classification engine for a personal job-search and finance inbox assistant.
            You will be given ONE email (sender, subject, body). Classify it strictly according to the
            rules below and respond with ONLY a single JSON object - no markdown, no explanation outside the JSON.

            ============================================================
            CATEGORIES (choose exactly one for "result")
            ============================================================

            1. RECRUITER_RESPONSE
               Use this ONLY when the email is a genuine response about a job application the user
               already submitted, sent by a recruiter, hiring manager, or company - NOT an automated
               portal confirmation. Examples that QUALIFY:
                 - Interview scheduled / invitation to interview
                 - Rejection ("we've decided to move forward with other candidates", etc.)
                 - Next round / assessment / coding test / task required
                 - Offer letter / offer discussion
                 - Any message that requires the user to take an action or make a decision about
                   an application already in progress

               Examples that DO NOT QUALIFY (these must be classified as IGNORED):
                 - "Your application has been received/submitted" from a job portal (LinkedIn,
                   Naukri, Indeed, Glassdoor, Workday, Greenhouse, Lever, etc.)
                 - "Thank you for applying" auto-replies from the company itself
                 - Job recommendation / job alert digest emails ("Jobs matching your profile")
                 - Marketing emails from job portals about premium features

            2. BANK_IMPORTANT
               Use this ONLY for genuinely important account/statement notifications directly from
               a bank or financial institution. Examples that QUALIFY:
                 - Monthly/periodic account statement generated and ready
                 - Important account alerts (e.g. large transaction, security alert, KYC required,
                   account will be blocked/deactivated)

               Examples that DO NOT QUALIFY (classify as IGNORED):
                 - Loan offers, pre-approved loan messages
                 - Credit card offers / promotions / cashback offers
                 - "Upgrade your account" / cross-sell marketing
                 - Generic newsletters from the bank

            3. IGNORED
               Everything else: all promotional emails, application confirmations, job alerts,
               newsletters, spam, unrelated personal/social emails, loan/credit-card offers, etc.
               When result is IGNORED, you may leave subtype-specific fields null, but you MUST
               still fill "reason" briefly explaining why it was ignored.

            ============================================================
            OUTPUT JSON SCHEMA (return exactly these fields)
            ============================================================
            {
              "result": "RECRUITER_RESPONSE" | "BANK_IMPORTANT" | "IGNORED",
              "subtype": one of [
                  "INTERVIEW_SCHEDULED","REJECTION","NEXT_ROUND","ASSESSMENT_REQUIRED","OFFER","RECRUITER_OTHER",
                  "STATEMENT_GENERATED","ACCOUNT_ALERT","BANK_OTHER"
                ] or null if result is IGNORED,
              "priority": "CRITICAL" | "HIGH" | "MEDIUM" or null if result is IGNORED,
              "importance": integer 0-100 (0 for IGNORED, otherwise how important/urgent this is),
              "actionRequired": true | false,
              "deadline": ISO-8601 datetime string if a specific date/time is mentioned (interview time,
                          assessment due date, statement date), otherwise null,
              "company": company name if identifiable (recruiter emails), otherwise null,
              "jobRole": job title if identifiable, otherwise null,
              "nextStep": short actionable instruction (e.g. "Join Google Meet at 10 AM"), otherwise null,
              "summary": one or two sentence plain-language summary of the email,
              "shouldNotify": true if result is RECRUITER_RESPONSE or BANK_IMPORTANT, false if IGNORED,
              "reason": brief explanation of why you classified it this way
            }

            Priority guidance:
              - CRITICAL: interview/deadline within the next 48 hours, or urgent bank security alert
              - HIGH: assessment/next-round with a deadline further out, offer letters
              - MEDIUM: rejections, statement generated, informational alerts with no immediate deadline

            Respond with ONLY the JSON object, nothing else.
            """;

    public OllamaAnalysisResult analyzeEmail(IncomingEmailRequest email) {
        String truncatedBody = truncate(email.getBodyText(), appProperties.getEmailProcessing().getMaxBodyChars());

        String userPrompt = """
                Sender name: %s
                Sender email: %s
                Subject: %s

                Body:
                %s
                """.formatted(
                nullToEmpty(email.getSenderName()),
                nullToEmpty(email.getSenderEmail()),
                nullToEmpty(email.getSubject()),
                truncatedBody
        );

        OllamaGenerateRequest request = OllamaGenerateRequest.builder()
                .model(appProperties.getOllama().getModel())
                .system(SYSTEM_PROMPT)
                .prompt(userPrompt)
                .format("json")
                .stream(false)
                .build();

        log.debug("Sending email '{}' to Ollama for analysis", email.getSubject());

        OllamaGenerateResponse rawResponse;
        try {
            ollamaLock.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for Ollama to be free", e);
        }
        try {
            rawResponse = ollamaWebClient.post()
                    .uri("/api/generate")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(OllamaGenerateResponse.class)
                    .block();
        } finally {
            ollamaLock.release();
        }

        if (rawResponse == null || rawResponse.getResponse() == null) {
            throw new IllegalStateException("Ollama returned an empty response for email: " + email.getGmailMessageId());
        }

        try {
            return objectMapper.readValue(rawResponse.getResponse(), OllamaAnalysisResult.class);
        } catch (Exception e) {
            log.error("Failed to parse Ollama JSON response: {}", rawResponse.getResponse(), e);
            throw new IllegalStateException("Could not parse AI analysis result", e);
        }
    }

    private String truncate(String text, int maxChars) {
        if (text == null) return "";
        return text.length() <= maxChars ? text : text.substring(0, maxChars) + "... [truncated]";
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}