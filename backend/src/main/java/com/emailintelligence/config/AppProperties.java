package com.emailintelligence.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {

    private Ollama ollama = new Ollama();
    private Groq groq = new Groq();
    private Telegram telegram = new Telegram();
    private EmailProcessing emailProcessing = new EmailProcessing();
    private Reminders reminders = new Reminders();
    private Digest digest = new Digest();
    private Cors cors = new Cors();

    @Getter
    @Setter
    public static class Ollama {
        private String baseUrl;
        private String model;
        private int timeoutSeconds;
    }

    @Getter
    @Setter
    public static class Groq {
        private String baseUrl;
        private String apiKey;
        private String model;
        private int timeoutSeconds;
    }

    @Getter
    @Setter
    public static class Telegram {
        private String botToken;
        private String chatId;
        private String apiBaseUrl;
    }

    @Getter
    @Setter
    public static class EmailProcessing {
        private int maxBodyChars;
        private boolean dedupeEnabled;
    }

    @Getter
    @Setter
    public static class Reminders {
        private boolean enabled;
        private int leadMinutes;        // how far before the deadline to nudge
        private long checkIntervalMs;   // how often the scheduler polls
    }

    @Getter
    @Setter
    public static class Digest {
        private boolean enabled;
        private String cron;    // e.g. "0 0 8 * * *" = 8:00 AM daily
        private String zone;    // e.g. "Asia/Kolkata"
    }

    @Getter
    @Setter
    public static class Cors {
        private java.util.List<String> allowedOrigins = new java.util.ArrayList<>();
    }
}