package com.emailintelligence.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for Ollama's /api/generate endpoint.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OllamaGenerateRequest {
    private String model;
    private String system;
    private String prompt;
    private String format;   // "json" forces Ollama to return valid JSON
    private boolean stream;
}
