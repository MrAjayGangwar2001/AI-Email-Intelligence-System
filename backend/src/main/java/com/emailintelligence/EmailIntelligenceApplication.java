package com.emailintelligence;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class EmailIntelligenceApplication {
    public static void main(String[] args) {
        SpringApplication.run(EmailIntelligenceApplication.class, args);
    }
}
