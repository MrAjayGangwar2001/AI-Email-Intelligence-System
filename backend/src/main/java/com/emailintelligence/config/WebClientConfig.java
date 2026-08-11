package com.emailintelligence.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient ollamaWebClient(AppProperties appProperties) {
        AppProperties.Ollama cfg = appProperties.getOllama();
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(cfg.getTimeoutSeconds() > 0 ? cfg.getTimeoutSeconds() : 30));

        return WebClient.builder()
                .baseUrl(cfg.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(c -> c.defaultCodecs().maxInMemorySize(5 * 1024 * 1024)) // 5MB, AI responses can be verbose
                .build();
    }

    @Bean
    public WebClient groqWebClient(AppProperties appProperties) {
        AppProperties.Groq cfg = appProperties.getGroq();
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(cfg.getTimeoutSeconds() > 0 ? cfg.getTimeoutSeconds() : 30));

        return WebClient.builder()
                .baseUrl(cfg.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + cfg.getApiKey())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(c -> c.defaultCodecs().maxInMemorySize(5 * 1024 * 1024))
                .build();
    }

    @Bean
    public WebClient telegramWebClient(AppProperties appProperties) {
        return WebClient.builder()
                .baseUrl(appProperties.getTelegram().getApiBaseUrl())
                .build();
    }
}