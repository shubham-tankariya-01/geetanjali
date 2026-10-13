package com.geetanjali.backend.config;

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
    // WHY: Cloud servers and Linux containers default to UTC. If we blindly check the current date
    // late at night in India, a server in UTC would record the previous calendar date. By injecting
    // a Clock bean fixed to "Asia/Kolkata", every service consistently calculates Indian Standard Time (IST).
    // Input: None.
    // Output: java.time.Clock bean.
    // @Bean = Tells Spring to instantiate, configure, and register this Clock object as a singleton bean.
    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of(appTimezone));
    }
}
