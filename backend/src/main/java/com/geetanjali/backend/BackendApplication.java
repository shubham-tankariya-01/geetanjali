package com.geetanjali.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;

// @SpringBootApplication = Tells Spring Boot that this is the main entry point and configuration class.
// In Express terms, this is like your main "app.js" or "server.js" where the web server is booted.
@SpringBootApplication
public class BackendApplication {

    // Main entry point method for the Java Virtual Machine (JVM).
    // Automatically loads environment variables from .env if present before booting Spring ApplicationContext.
    public static void main(String[] args) {
        loadDotEnvIfPresent();
        SpringApplication.run(BackendApplication.class, args);
    }

    /**
     * Checks for a .env file in the current working directory, project directory, or parent directory.
     * Populates System properties for any variables not already exported in the OS environment.
     */
    private static void loadDotEnvIfPresent() {
        String[] candidatePaths = {
            ".env",
            "backend/.env",
            "../.env",
            System.getProperty("user.dir") + "/.env",
            System.getProperty("user.dir") + "/backend/.env"
        };

        for (String path : candidatePaths) {
            File envFile = new File(path);
            if (envFile.exists() && envFile.isFile() && envFile.canRead()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(envFile, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) {
                            continue;
                        }
                        int eqIdx = line.indexOf('=');
                        if (eqIdx > 0) {
                            String key = line.substring(0, eqIdx).trim();
                            String val = line.substring(eqIdx + 1).trim();
                            // Strip surrounding single or double quotes if present
                            if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
                                if (val.length() >= 2) {
                                    val = val.substring(1, val.length() - 1);
                                }
                            }
                            // Only set if not already present in OS environment or system properties
                            if (System.getenv(key) == null && System.getProperty(key) == null) {
                                System.setProperty(key, val);
                            }
                        }
                    }
                } catch (Exception ignored) {
                    // Fail gracefully if .env cannot be parsed
                }
                break;
            }
        }
    }
}
