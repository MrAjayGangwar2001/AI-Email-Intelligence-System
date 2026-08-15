package com.emailintelligence.service;

import com.emailintelligence.config.AppProperties;
import com.emailintelligence.dto.GroqChatMessage;
import com.emailintelligence.dto.GroqChatRequest;
import com.emailintelligence.dto.GroqChatResponse;
import com.emailintelligence.dto.IncomingEmailRequest;
import com.emailintelligence.dto.OllamaAnalysisResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

/**
 * Classifies emails using Groq's free, fast cloud LLM API instead of a local
 * Ollama instance. This avoids the RAM/timeout problems of running an LLM
 * locally on a resource-constrained machine, and comfortably handles bursts
 * of multiple emails arriving at once (no local queuing/semaphore needed).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GroqService {

    private final WebClient groqWebClient;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
            You are an email classification engine for a personal inbox assistant. The user is
            currently job-hunting, so job-related and financial emails matter most, but you should
            also catch other genuinely important emails (personal, business, deliveries) so nothing
            important slips through.
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

            3. PERSONAL_IMPORTANT
               Genuine, personally-written messages from a family member, friend, or known
               individual contact - NOT mailing lists, NOT automated, NOT a stranger's cold outreach.
               Examples that QUALIFY:
                 - A personal message from someone the user clearly knows, with real content
                   (an invitation, a request, news, a personal update)
               Examples that DO NOT QUALIFY (classify as IGNORED):
                 - Newsletters, social media notifications, group/community mailing lists
                 - Generic "someone you may know" or platform-generated suggestion emails

            4. COMPANY_BUSINESS
               Genuine business/work communication from a client, vendor, partner, or an official
               internal/company source - something that likely needs the user's attention or a reply.
               Examples that QUALIFY:
                 - A client or vendor asking a question, sharing a document, or requesting action
                 - Official company/HR/admin communication requiring acknowledgment or action
               Examples that DO NOT QUALIFY (classify as IGNORED):
                 - Marketing/sales outreach from unrelated companies (cold sales pitches)
                 - Internal automated system notifications with no action needed
                 - Newsletters from SaaS tools, generic product update emails

            5. DELIVERY_UPDATE
               Genuinely important package/order delivery status that needs attention.
               Examples that QUALIFY:
                 - Delivery delayed, failed delivery attempt, address/OTP issue needing action
                 - Out for delivery / arriving today (time-sensitive)
               Examples that DO NOT QUALIFY (classify as IGNORED):
                 - Routine "order confirmed" / "order shipped" with no action needed and no urgency
                 - Marketing emails from delivery/shopping platforms

            6. OTHER_IMPORTANT
               Use this ONLY as a safety net: the email is clearly important and the user would
               likely want to know about it or act on it, but it doesn't fit cleanly into any
               category above (e.g. legal/government notices, exam/education results, urgent
               official communication of some other kind). Do not overuse this - only for emails
               that are genuinely important, not simply "unclear".

            7. IGNORED
               Everything else: all promotional emails, application confirmations, job alerts,
               newsletters, spam, unrelated automated notifications, loan/credit-card offers,
               routine confirmations with no action needed, etc.
               When result is IGNORED, you may leave subtype-specific fields null, but you MUST
               still fill "reason" briefly explaining why it was ignored.

            ============================================================
            OUTPUT JSON SCHEMA (return exactly these fields)
            ============================================================
            {
              "result": "RECRUITER_RESPONSE" | "BANK_IMPORTANT" | "PERSONAL_IMPORTANT" | "COMPANY_BUSINESS" | "DELIVERY_UPDATE" | "OTHER_IMPORTANT" | "IGNORED",
              "subtype": one of [
                  "INTERVIEW_SCHEDULED","REJECTION","NEXT_ROUND","ASSESSMENT_REQUIRED","OFFER","RECRUITER_OTHER",
                  "STATEMENT_GENERATED","ACCOUNT_ALERT","BANK_OTHER",
                  "FAMILY_FRIEND","PERSONAL_OTHER",
                  "CLIENT_VENDOR","WORK_OFFICIAL","BUSINESS_OTHER",
                  "DELIVERY_ISSUE","DELIVERY_STATUS",
                  "OTHER_IMPORTANT_MISC"
                ] or null if result is IGNORED,
              "priority": "CRITICAL" | "HIGH" | "MEDIUM" or null if result is IGNORED,
              "importance": integer 0-100 (0 for IGNORED, otherwise how important/urgent this is),
              "actionRequired": true | false,
              "deadline": ISO-8601 datetime string if a specific date/time is mentioned (interview time,
                          assessment due date, statement date, delivery window), otherwise null,
              "company": company name if identifiable (recruiter/business/bank emails), otherwise null,
              "jobRole": job title if identifiable (recruiter emails only), otherwise null,
              "nextStep": short actionable instruction (e.g. "Join Google Meet at 10 AM"), otherwise null,
              "summary": one or two sentence plain-language summary of the email,
              "shouldNotify": true if result is anything other than IGNORED, false if IGNORED,
              "reason": brief explanation of why you classified it this way
            }

            Priority guidance:
              - CRITICAL: interview/deadline within the next 48 hours, urgent bank security alert,
                          urgent personal/family matter, failed delivery needing same-day action
              - HIGH: assessment/next-round with a deadline further out, offer letters, time-sensitive
                      business requests, delivery delayed/issue
              - MEDIUM: rejections, statement generated, informational alerts with no immediate deadline,
                        routine personal/business messages, delivery status updates

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

        GroqChatRequest request = GroqChatRequest.builder()
                .model(appProperties.getGroq().getModel())
                .temperature(0.2)
                .responseFormat(GroqChatRequest.ResponseFormat.builder().type("json_object").build())
                .messages(List.of(
                        GroqChatMessage.builder().role("system").content(SYSTEM_PROMPT).build(),
                        GroqChatMessage.builder().role("user").content(userPrompt).build()
                ))
                .build();

        log.debug("Sending email '{}' to Groq for analysis", email.getSubject());

        GroqChatResponse rawResponse = groqWebClient.post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(GroqChatResponse.class)
                .block();

        if (rawResponse == null || rawResponse.getChoices() == null || rawResponse.getChoices().isEmpty()) {
            throw new IllegalStateException("Groq returned an empty response for email: " + email.getGmailMessageId());
        }

        String content = rawResponse.getChoices().get(0).getMessage().getContent();

        try {
            return objectMapper.readValue(content, OllamaAnalysisResult.class);
        } catch (Exception e) {
            log.error("Failed to parse Groq JSON response: {}", content, e);
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