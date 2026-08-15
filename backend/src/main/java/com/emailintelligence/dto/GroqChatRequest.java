package com.emailintelligence.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Request body for Groq's /openai/v1/chat/completions endpoint
 * (OpenAI-compatible chat completions format).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroqChatRequest {
    private String model;
    private List<GroqChatMessage> messages;
    private double temperature;

    @JsonProperty("response_format")
    private ResponseFormat responseFormat;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ResponseFormat {
        private String type; // "json_object" forces valid JSON output
    }
}