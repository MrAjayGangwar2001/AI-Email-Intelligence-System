package com.emailintelligence.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Structure of the JSON we instruct Ollama to return for every email.
 * See OllamaService for the exact prompt that produces this shape.
 *
 * "result" is the broad decision: RECRUITER_RESPONSE / BANK_IMPORTANT / IGNORED
 * "subtype" is only meaningful when result != IGNORED
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OllamaAnalysisResult {

    private String result;              // RECRUITER_RESPONSE | BANK_IMPORTANT | IGNORED
    private String subtype;             // e.g. INTERVIEW_SCHEDULED, REJECTION, STATEMENT_GENERATED...
    private String priority;            // CRITICAL | HIGH | MEDIUM (only relevant if not IGNORED)
    private Integer importance;         // 0-100
    private Boolean actionRequired;
    private String deadline;            // ISO-8601 datetime string, nullable
    private String company;
    private String jobRole;
    private String nextStep;
    private String summary;
    private Boolean shouldNotify;
    private String reason;              // why the AI classified it this way
}
