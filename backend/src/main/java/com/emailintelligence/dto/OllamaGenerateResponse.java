package com.emailintelligence.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Raw response shape from Ollama's /api/generate endpoint.
 * The actual AI JSON we care about is inside "response" (as a string).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OllamaGenerateResponse {
    private String model;
    private String response;
    private boolean done;
}
