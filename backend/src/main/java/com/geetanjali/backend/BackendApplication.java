package com.geetanjali.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// @SpringBootApplication = Tells Spring Boot that this is the main entry point and configuration class.
// In Express terms, this is like your main "app.js" or "server.js" where the web server is booted.
@SpringBootApplication
public class BackendApplication {

    // Main entry point method for the Java Virtual Machine (JVM).
    // Input: Command line arguments array.
    // Output: None (void). Boots up the Spring ApplicationContext.
    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
}
