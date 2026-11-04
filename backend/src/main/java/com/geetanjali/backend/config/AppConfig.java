package com.geetanjali.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;
import java.time.ZoneId;

// AppConfig: Application-wide configuration class providing foundational Spring beans.
// In Express, this is like setting up app-level singletons or utilities.
// @Configuration = Tells Spring that this class defines @Bean producer methods.
@Configuration
public class AppConfig {

    // Reads the configured timezone from application.properties ("Asia/Kolkata").
    @Value("${app.timezone:Asia/Kolkata}")
    private String appTimezone;

    // Creates and registers a Clock bean locked to the shop's timezone (Asia/Kolkata).
    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of(appTimezone));
    }

    // Shared JSON serializer/deserializer bean for API controllers and AI Assistant
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
